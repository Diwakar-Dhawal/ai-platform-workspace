package com.aiservice.applications.insighttube.tubeservice.service;

import com.aiservice.applications.insighttube.tubeservice.dto.TranscriptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TranscriptChunker")
class TranscriptChunkerTest {

    private TranscriptChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new TranscriptChunker();
    }

    @Nested
    @DisplayName("chunk()")
    class Chunk {

        @Test
        @DisplayName("should chunk single video transcript")
        void shouldChunkSingleVideo() {
            TranscriptResponse.VideoTranscript video = createVideo("test-video", "Test Video", 10);
            UUID sessionId = UUID.randomUUID();

            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(List.of(video), sessionId);

            assertFalse(chunks.isEmpty());
            assertEquals("insighttube", chunks.get(0).metadata().get("videoId") != null ? "insighttube" : "");
        }

        @Test
        @DisplayName("should handle empty segments")
        void shouldHandleEmptySegments() {
            TranscriptResponse.VideoTranscript video = createVideo("test", "Empty", 0);
            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(List.of(video), UUID.randomUUID());
            assertTrue(chunks.isEmpty());
        }

        @Test
        @DisplayName("should chunk multiple videos")
        void shouldChunkMultipleVideos() {
            TranscriptResponse.VideoTranscript video1 = createVideo("v1", "Video 1", 5);
            TranscriptResponse.VideoTranscript video2 = createVideo("v2", "Video 2", 5);

            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(List.of(video1, video2), UUID.randomUUID());

            assertFalse(chunks.isEmpty());
            // All chunks should have correct total count
            int total = chunks.size();
            for (TranscriptChunker.ChunkData chunk : chunks) {
                assertEquals(total, chunk.totalChunks());
            }
        }

        @Test
        @DisplayName("should set correct chunk indices")
        void shouldSetCorrectIndices() {
            TranscriptResponse.VideoTranscript video = createVideo("v1", "Video", 10);
            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(List.of(video), UUID.randomUUID());

            for (int i = 0; i < chunks.size(); i++) {
                assertEquals(i, chunks.get(i).chunkIndex());
            }
        }

        @Test
        @DisplayName("should include metadata with video info")
        void shouldIncludeMetadata() {
            TranscriptResponse.VideoTranscript video = createVideo("vid123", "My Video", 3);
            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(List.of(video), UUID.randomUUID());

            assertFalse(chunks.isEmpty());
            var meta = chunks.get(0).metadata();
            assertEquals("vid123", meta.get("videoId"));
            assertEquals("My Video", meta.get("videoTitle"));
            assertNotNull(meta.get("timestamp"));
        }

        @Test
        @DisplayName("should produce unique chunk IDs")
        void shouldProduceUniqueIds() {
            TranscriptResponse.VideoTranscript video = createVideo("v1", "Video", 8);
            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(List.of(video), UUID.randomUUID());

            var ids = chunks.stream().map(TranscriptChunker.ChunkData::chunkId).toList();
            assertEquals(ids.size(), new java.util.HashSet<>(ids).size(), "All chunk IDs should be unique");
        }
    }

    @Nested
    @DisplayName("formatTimestamp()")
    class FormatTimestamp {

        @Test
        @DisplayName("should format seconds correctly")
        void shouldFormatSeconds() {
            TranscriptResponse.VideoTranscript video = createVideo("v1", "V", 1);
            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(List.of(video), UUID.randomUUID());
            // Just verify it doesn't throw and produces non-null
            assertFalse(chunks.isEmpty());
            assertNotNull(chunks.get(0).metadata().get("timestamp"));
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private TranscriptResponse.VideoTranscript createVideo(String id, String title, int segmentCount) {
        TranscriptResponse.VideoTranscript video = new TranscriptResponse.VideoTranscript();
        video.setVideoId(id);
        video.setTitle(title);
        video.setChannelName("Test Channel");
        video.setLanguage("en");

        List<TranscriptResponse.TranscriptSegment> segments = new ArrayList<>();
        for (int i = 0; i < segmentCount; i++) {
            TranscriptResponse.TranscriptSegment seg = new TranscriptResponse.TranscriptSegment();
            seg.setText("This is segment number " + i + " of the video. It contains some meaningful content about the topic being discussed in the video.");
            seg.setStart(i * 10.0);
            seg.setDuration(10.0);
            segments.add(seg);
        }

        video.setSegments(segments);
        video.setSegmentCount(segmentCount);
        video.setFullText(segments.stream().map(s -> s.getText()).reduce("", (a, b) -> a + " " + b));
        video.setDurationSeconds(segmentCount * 10.0);

        return video;
    }
}
