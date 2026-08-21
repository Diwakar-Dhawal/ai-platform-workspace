"""
YouTube Transcript Extraction Microservice

Extracts transcripts from YouTube videos, playlists, and channels.
Caches results in Redis to avoid repeated extraction.

Endpoints:
  POST /extract         — Extract transcript from a YouTube URL
  GET  /health          — Health check
"""

import os
import re
import json
import hashlib
from typing import Optional
from datetime import datetime

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from youtube_transcript_api import YouTubeTranscriptApi
import redis
import httpx

app = FastAPI(
    title="Transcript Service",
    description="YouTube transcript extraction with Redis caching",
    version="0.0.1",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Redis connection
REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
redis_client = redis.from_url(REDIS_URL, decode_responses=True)

# YouTube Data API (free tier, optional — for metadata)
YOUTUBE_API_KEY = os.getenv("YOUTUBE_API_KEY", "")


# ─── Models ────────────────────────────────────────────────────────────

class ExtractRequest(BaseModel):
    url: str = Field(..., description="YouTube video, playlist, or channel URL")
    languages: list[str] = Field(
        default=["en"],
        description="Preferred transcript languages (ISO 639-1 codes)"
    )


class TranscriptSegment(BaseModel):
    text: str
    start: float
    duration: float


class VideoTranscript(BaseModel):
    video_id: str
    title: str
    channel_name: str
    duration_seconds: float
    language: str
    segments: list[TranscriptSegment]
    full_text: str
    segment_count: int


class ExtractResponse(BaseModel):
    status: str
    url: str
    content_type: str  # video, playlist, channel
    videos: list[VideoTranscript]
    total_videos: int
    total_segments: int
    cached: bool


# ─── URL Parsing ────────────────────────────────────────────────────────

def parse_youtube_url(url: str) -> dict:
    """Parse YouTube URL and extract video ID, playlist ID, or channel ID."""
    patterns = {
        "video": [
            r"(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/)([a-zA-Z0-9_-]{11})",
            r"youtube\.com/shorts/([a-zA-Z0-9_-]{11})",
        ],
        "playlist": [
            r"youtube\.com/playlist\?list=([a-zA-Z0-9_-]+)",
        ],
        "channel": [
            r"youtube\.com/channel/([a-zA-Z0-9_-]+)",
            r"youtube\.com/@([a-zA-Z0-9_-]+)",
            r"youtube\.com/c/([a-zA-Z0-9_-]+)",
            r"youtube\.com/user/([a-zA-Z0-9_-]+)",
        ],
    }

    for content_type, regexes in patterns.items():
        for pattern in regexes:
            match = re.search(pattern, url)
            if match:
                return {"type": content_type, "id": match.group(1), "url": url}

    raise ValueError(f"Could not parse YouTube URL: {url}")


def get_cache_key(url: str, languages: list[str]) -> str:
    """Generate a cache key for a transcript request."""
    raw = f"{url}:{':'.join(sorted(languages))}"
    return f"transcript:{hashlib.sha256(raw.encode()).hexdigest()[:16]}"


# ─── Transcript Extraction ─────────────────────────────────────────────

def extract_video_transcript(
    video_id: str,
    languages: list[str],
    title: str = "",
    channel_name: str = "",
) -> VideoTranscript:
    """Extract transcript for a single video."""
    try:
        ytt_api = YouTubeTranscriptApi()
        transcript = ytt_api.fetch(video_id, languages=languages)

        segments = [
            TranscriptSegment(
                text=snippet.text,
                start=snippet.start,
                duration=snippet.duration,
            )
            for snippet in transcript.snippets
        ]

        full_text = " ".join(s.text for s in segments)
        total_duration = (
            segments[-1].start + segments[-1].duration if segments else 0
        )

        return VideoTranscript(
            video_id=video_id,
            title=title or f"Video {video_id}",
            channel_name=channel_name or "Unknown",
            duration_seconds=total_duration,
            language=transcript.language,
            segments=segments,
            full_text=full_text,
            segment_count=len(segments),
        )
    except Exception as e:
        raise HTTPException(
            status_code=422,
            detail=f"Could not extract transcript for video {video_id}: {str(e)}"
        )


def get_playlist_video_ids(playlist_id: str) -> list[str]:
    """Get all video IDs from a playlist using YouTube Data API."""
    if not YOUTUBE_API_KEY:
        raise HTTPException(
            status_code=500,
            detail="YOUTUBE_API_KEY not configured. Cannot fetch playlist."
        )

    video_ids = []
    next_page_token = None

    with httpx.Client() as client:
        while True:
            params = {
                "part": "contentDetails",
                "playlistId": playlist_id,
                "maxResults": 50,
                "key": YOUTUBE_API_KEY,
            }
            if next_page_token:
                params["pageToken"] = next_page_token

            resp = client.get(
                "https://www.googleapis.com/youtube/v3/playlistItems",
                params=params,
            )
            data = resp.json()

            for item in data.get("items", []):
                vid = item["contentDetails"]["videoId"]
                video_ids.append(vid)

            next_page_token = data.get("nextPageToken")
            if not next_page_token:
                break

    return video_ids


def get_video_metadata(video_id: str) -> dict:
    """Get video title and channel name from YouTube Data API."""
    if not YOUTUBE_API_KEY:
        return {"title": f"Video {video_id}", "channel_name": "Unknown"}

    with httpx.Client() as client:
        resp = client.get(
            "https://www.googleapis.com/youtube/v3/videos",
            params={
                "part": "snippet",
                "id": video_id,
                "key": YOUTUBE_API_KEY,
            },
        )
        data = resp.json()
        if data.get("items"):
            snippet = data["items"][0]["snippet"]
            return {
                "title": snippet.get("title", f"Video {video_id}"),
                "channel_name": snippet.get("channelTitle", "Unknown"),
            }
    return {"title": f"Video {video_id}", "channel_name": "Unknown"}


# ─── Endpoints ──────────────────────────────────────────────────────────

@app.get("/health")
def health():
    redis_ok = False
    try:
        redis_client.ping()
        redis_ok = True
    except Exception:
        pass

    return {
        "service": "transcript-service",
        "status": "UP",
        "version": "0.0.1",
        "redis": "connected" if redis_ok else "disconnected",
    }


@app.post("/extract", response_model=ExtractResponse)
def extract_transcript(request: ExtractRequest):
    """Extract transcript from a YouTube URL (video, playlist, or channel)."""
    # Parse URL
    try:
        parsed = parse_youtube_url(request.url)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    content_type = parsed["type"]
    content_id = parsed["id"]

    # Check cache
    cache_key = get_cache_key(request.url, request.languages)
    cached = redis_client.get(cache_key)
    if cached:
        data = json.loads(cached)
        data["cached"] = True
        return ExtractResponse(**data)

    # Extract based on type
    videos = []

    if content_type == "video":
        meta = get_video_metadata(content_id)
        transcript = extract_video_transcript(
            content_id,
            request.languages,
            title=meta["title"],
            channel_name=meta["channel_name"],
        )
        videos = [transcript]

    elif content_type == "playlist":
        video_ids = get_playlist_video_ids(content_id)
        for vid in video_ids[:50]:  # Limit to 50 videos
            meta = get_video_metadata(vid)
            transcript = extract_video_transcript(
                vid,
                request.languages,
                title=meta["title"],
                channel_name=meta["channel_name"],
            )
            videos.append(transcript)

    elif content_type == "channel":
        # Channel extraction requires YouTube Data API
        raise HTTPException(
            status_code=501,
            detail="Channel extraction not yet implemented. Use playlist or video URL."
        )

    total_segments = sum(v.segment_count for v in videos)

    response_data = {
        "status": "success",
        "url": request.url,
        "content_type": content_type,
        "videos": [v.model_dump() for v in videos],
        "total_videos": len(videos),
        "total_segments": total_segments,
        "cached": False,
    }

    # Cache for 7 days (YouTube videos don't change)
    redis_client.setex(cache_key, 60 * 60 * 24 * 7, json.dumps(response_data))

    return ExtractResponse(**response_data)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8085)
