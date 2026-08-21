package com.aiservice.applications.insighttube.tubeservice.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IngestProgressTest {

    @Nested
    @DisplayName("started()")
    class Started {

        @Test
        @DisplayName("should create initial progress with PENDING status")
        void shouldCreateInitialProgress() {
            UUID sessionId = UUID.randomUUID();
            IngestProgress progress = IngestProgress.started(sessionId);

            assertEquals(sessionId, progress.getContentSessionId());
            assertEquals("PENDING", progress.getStatus());
            assertEquals("Queued for processing", progress.getCurrentStep());
            assertEquals(0, progress.getPercentComplete());
            assertEquals(0, progress.getTotalChunks());
            assertEquals(0, progress.getChunksEmbedded());
            assertEquals(0, progress.getChunksStored());
            assertNotNull(progress.getStartedAt());
            assertNull(progress.getCompletedAt());
        }
    }

    @Nested
    @DisplayName("withExtractingTranscript()")
    class ExtractingTranscript {

        @Test
        @DisplayName("should update status to EXTRACTING_TRANSCRIPT")
        void shouldUpdateStatus() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withExtractingTranscript();

            assertEquals("EXTRACTING_TRANSCRIPT", progress.getStatus());
            assertEquals("Extracting transcript from YouTube...", progress.getCurrentStep());
            assertEquals(5, progress.getPercentComplete());
        }
    }

    @Nested
    @DisplayName("withChunking()")
    class Chunking {

        @Test
        @DisplayName("should update status with segment and video counts")
        void shouldUpdateWithCounts() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withChunking(120, 3);

            assertEquals("CHUNKING", progress.getStatus());
            assertEquals(120, progress.getSegmentCount());
            assertEquals(3, progress.getVideoCount());
            assertEquals(15, progress.getPercentComplete());
        }
    }

    @Nested
    @DisplayName("withEmbedding()")
    class Embedding {

        @Test
        @DisplayName("should update status with total chunks and title")
        void shouldUpdateWithChunkCount() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(50, "React Tutorial");

            assertEquals("EMBEDDING", progress.getStatus());
            assertEquals(50, progress.getTotalChunks());
            assertEquals("React Tutorial", progress.getTitle());
            assertEquals(20, progress.getPercentComplete());
        }
    }

    @Nested
    @DisplayName("withChunkEmbedded()")
    class ChunkEmbedded {

        @Test
        @DisplayName("should increment chunksEmbedded and update percent")
        void shouldIncrementAndCalculatePercent() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(10, "Test Video");

            // Embed first chunk
            progress.withChunkEmbedded();
            assertEquals(1, progress.getChunksEmbedded());
            // 20 + (1/10 * 50) = 25%
            assertEquals(25, progress.getPercentComplete());
        }

        @Test
        @DisplayName("should calculate percent correctly at 50% of chunks")
        void shouldCalculateAtHalfWay() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(10, "Test Video");

            // Embed 5 chunks (half)
            for (int i = 0; i < 5; i++) {
                progress.withChunkEmbedded();
            }

            assertEquals(5, progress.getChunksEmbedded());
            // 20 + (5/10 * 50) = 45%
            assertEquals(45, progress.getPercentComplete());
        }

        @Test
        @DisplayName("should calculate percent correctly at 100% of chunks")
        void shouldCalculateAtComplete() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(10, "Test Video");

            for (int i = 0; i < 10; i++) {
                progress.withChunkEmbedded();
            }

            assertEquals(10, progress.getChunksEmbedded());
            // 20 + (10/10 * 50) = 70%
            assertEquals(70, progress.getPercentComplete());
        }

        @Test
        @DisplayName("should update current step message")
        void shouldUpdateStepMessage() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(5, "Test Video");

            progress.withChunkEmbedded();
            assertEquals("Embedding chunks... (1/5)", progress.getCurrentStep());

            progress.withChunkEmbedded();
            assertEquals("Embedding chunks... (2/5)", progress.getCurrentStep());
        }
    }

    @Nested
    @DisplayName("withChunkStored()")
    class ChunkStored {

        @Test
        @DisplayName("should increment chunksStored and update percent")
        void shouldIncrementAndCalculatePercent() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(10, "Test Video")
                    .withStoring();

            progress.withChunkStored();
            assertEquals(1, progress.getChunksStored());
            // 75 + (1/10 * 20) = 77%
            assertEquals(77, progress.getPercentComplete());
        }

        @Test
        @DisplayName("should calculate percent correctly at 100% of chunks")
        void shouldCalculateAtComplete() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(10, "Test Video")
                    .withStoring();

            for (int i = 0; i < 10; i++) {
                progress.withChunkStored();
            }

            assertEquals(10, progress.getChunksStored());
            // 75 + (10/10 * 20) = 95%
            assertEquals(95, progress.getPercentComplete());
        }
    }

    @Nested
    @DisplayName("withCompleted()")
    class Completed {

        @Test
        @DisplayName("should set status to COMPLETED with 100%")
        void shouldComplete() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withCompleted();

            assertEquals("COMPLETED", progress.getStatus());
            assertEquals(100, progress.getPercentComplete());
            assertNotNull(progress.getCompletedAt());
            assertNull(progress.getEstimatedTimeRemainingSeconds());
        }
    }

    @Nested
    @DisplayName("withFailed()")
    class Failed {

        @Test
        @DisplayName("should set status to FAILED with error message")
        void shouldFail() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withFailed("YouTube API rate limited");

            assertEquals("FAILED", progress.getStatus());
            assertEquals("YouTube API rate limited", progress.getErrorMessage());
            assertNotNull(progress.getCompletedAt());
        }
    }

    @Nested
    @DisplayName("Full pipeline flow")
    class FullPipeline {

        @Test
        @DisplayName("should track complete ingest lifecycle")
        void shouldTrackFullLifecycle() {
            UUID sessionId = UUID.randomUUID();

            // Start
            IngestProgress progress = IngestProgress.started(sessionId);
            assertEquals("PENDING", progress.getStatus());
            assertEquals(0, progress.getPercentComplete());

            // Extract transcript
            progress.withExtractingTranscript();
            assertEquals("EXTRACTING_TRANSCRIPT", progress.getStatus());

            // Chunk
            progress.withChunking(100, 2);
            assertEquals("CHUNKING", progress.getStatus());

            // Embed
            progress.withEmbedding(20, "My Video");
            assertEquals("EMBEDDING", progress.getStatus());
            assertEquals(20, progress.getTotalChunks());

            // Embed first 10 chunks
            for (int i = 0; i < 10; i++) {
                progress.withChunkEmbedded();
            }
            assertEquals(10, progress.getChunksEmbedded());
            assertTrue(progress.getPercentComplete() > 20);
            assertTrue(progress.getPercentComplete() < 70);

            // Store
            progress.withStoring();
            assertEquals("STORING", progress.getStatus());

            // Store first 10 chunks
            for (int i = 0; i < 10; i++) {
                progress.withChunkStored();
            }

            // Complete remaining
            for (int i = 10; i < 20; i++) {
                progress.withChunkEmbedded();
                progress.withChunkStored();
            }

            // Complete
            progress.withCompleted();
            assertEquals("COMPLETED", progress.getStatus());
            assertEquals(100, progress.getPercentComplete());
            assertEquals(20, progress.getChunksEmbedded());
            assertEquals(20, progress.getChunksStored());
            assertNotNull(progress.getCompletedAt());
        }

        @Test
        @DisplayName("should track failure mid-pipeline")
        void shouldTrackFailureMidPipeline() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withExtractingTranscript()
                    .withChunking(50, 1)
                    .withEmbedding(10, "Video")
                    .withChunkEmbedded()
                    .withChunkEmbedded()
                    .withFailed("Gemini API timeout");

            assertEquals("FAILED", progress.getStatus());
            assertEquals(2, progress.getChunksEmbedded());
            assertEquals("Gemini API timeout", progress.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Estimated time remaining")
    class EstimatedTime {

        @Test
        @DisplayName("should calculate estimated time during embedding")
        void shouldCalculateEmbeddingTime() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(10, "Test");

            // No estimate before any chunks
            assertNull(progress.getEstimatedTimeRemainingSeconds());

            // Embed one chunk — estimate should appear
            progress.withChunkEmbedded();
            assertNotNull(progress.getEstimatedTimeRemainingSeconds());
            // 9 remaining × 1.9s = ~17s
            assertTrue(progress.getEstimatedTimeRemainingSeconds() > 15);
            assertTrue(progress.getEstimatedTimeRemainingSeconds() < 20);
        }

        @Test
        @DisplayName("should clear estimated time on completion")
        void shouldClearOnCompletion() {
            IngestProgress progress = IngestProgress.started(UUID.randomUUID())
                    .withEmbedding(5, "Test");

            progress.withChunkEmbedded();
            assertNotNull(progress.getEstimatedTimeRemainingSeconds());

            progress.withCompleted();
            assertNull(progress.getEstimatedTimeRemainingSeconds());
        }
    }
}
