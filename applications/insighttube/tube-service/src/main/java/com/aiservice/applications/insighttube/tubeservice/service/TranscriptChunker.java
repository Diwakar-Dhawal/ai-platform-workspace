package com.aiservice.applications.insighttube.tubeservice.service;

import com.aiservice.applications.insighttube.tubeservice.dto.TranscriptResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chunks YouTube transcripts into embeddable pieces.
 *
 * Strategy: Group consecutive transcript segments into chunks of ~500 tokens,
 * with 100-token overlap for context continuity.
 *
 * Each chunk carries metadata: videoId, videoTitle, channelName, timestamp.
 */
@Slf4j
@Service
public class TranscriptChunker {

    private static final int TARGET_CHUNK_SIZE = 500;  // ~tokens
    private static final int OVERLAP_SIZE = 100;        // ~tokens
    private static final int CHARS_PER_TOKEN = 4;       // rough estimate

    public record ChunkData(
            UUID chunkId,
            String content,
            int chunkIndex,
            int totalChunks,
            Map<String, Object> metadata
    ) {}

    /**
     * Chunk a list of video transcripts into embeddable pieces.
     */
    public List<ChunkData> chunk(
            List<TranscriptResponse.VideoTranscript> videos,
            UUID sessionId
    ) {
        List<ChunkData> allChunks = new ArrayList<>();

        for (TranscriptResponse.VideoTranscript video : videos) {
            List<ChunkData> videoChunks = chunkSingleVideo(video, sessionId);
            allChunks.addAll(videoChunks);
        }

        // Update totalChunks for all chunks
        int total = allChunks.size();
        List<ChunkData> updated = new ArrayList<>();
        for (ChunkData chunk : allChunks) {
            updated.add(new ChunkData(
                    chunk.chunkId(),
                    chunk.content(),
                    chunk.chunkIndex(),
                    total,
                    chunk.metadata()
            ));
        }

        log.info("Chunked {} videos into {} chunks", videos.size(), total);
        return updated;
    }

    private List<ChunkData> chunkSingleVideo(
            TranscriptResponse.VideoTranscript video,
            UUID sessionId
    ) {
        List<ChunkData> chunks = new ArrayList<>();
        List<TranscriptResponse.TranscriptSegment> segments = video.getSegments();

        if (segments == null || segments.isEmpty()) {
            return chunks;
        }

        // Group segments into chunks
        StringBuilder currentChunk = new StringBuilder();
        int chunkStart = 0;
        int segmentStartIdx = 0;

        for (int i = 0; i < segments.size(); i++) {
            TranscriptResponse.TranscriptSegment segment = segments.get(i);
            String segmentText = segment.getText();
            int segmentTokens = segmentText.length() / CHARS_PER_TOKEN;

            // Check if adding this segment would exceed target size
            int currentTokens = currentChunk.length() / CHARS_PER_TOKEN;
            if (currentTokens + segmentTokens > TARGET_CHUNK_SIZE && currentChunk.length() > 0) {
                // Save current chunk
                chunks.add(createChunk(
                        currentChunk.toString().trim(),
                        chunks.size(),
                        sessionId,
                        video,
                        segments.get(segmentStartIdx).getStart()
                ));

                // Start new chunk with overlap (go back a few segments)
                currentChunk = new StringBuilder();
                int overlapSegments = Math.max(1, OVERLAP_SIZE / Math.max(1, segmentTokens));
                segmentStartIdx = Math.max(segmentStartIdx, i - overlapSegments);

                for (int j = segmentStartIdx; j < i; j++) {
                    currentChunk.append(segments.get(j).getText()).append(" ");
                }
            }

            currentChunk.append(segmentText).append(" ");
        }

        // Save last chunk
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(
                    currentChunk.toString().trim(),
                    chunks.size(),
                    sessionId,
                    video,
                    segments.get(segmentStartIdx).getStart()
            ));
        }

        return chunks;
    }

    private ChunkData createChunk(
            String content,
            int index,
            UUID sessionId,
            TranscriptResponse.VideoTranscript video,
            double timestamp
    ) {
        Map<String, Object> metadata = Map.of(
                "videoId", video.getVideoId() != null ? video.getVideoId() : "",
                "videoTitle", video.getTitle() != null ? video.getTitle() : "",
                "channelName", video.getChannelName() != null ? video.getChannelName() : "",
                "timestamp", formatTimestamp(timestamp),
                "timestampSeconds", timestamp
        );

        return new ChunkData(
                UUID.randomUUID(),
                content,
                index,
                0,  // Will be updated later
                metadata
        );
    }

    private String formatTimestamp(double seconds) {
        int totalSeconds = (int) seconds;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }
}
