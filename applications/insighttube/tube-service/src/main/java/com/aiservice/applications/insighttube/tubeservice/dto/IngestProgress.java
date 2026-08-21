package com.aiservice.applications.insighttube.tubeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Real-time progress of a content ingestion pipeline.
 * Frontend polls this endpoint every 2 seconds during ingest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestProgress {

    private UUID contentSessionId;

    /** Overall status: PENDING, EXTRACTING_TRANSCRIPT, CHUNKING, EMBEDDING, STORING, COMPLETED, FAILED */
    private String status;

    /** Current step description for UI display */
    private String currentStep;

    /** 0-100 percent complete */
    private int percentComplete;

    /** Total chunks created */
    private int totalChunks;

    /** Chunks successfully embedded */
    private int chunksEmbedded;

    /** Chunks successfully stored in vector DB */
    private int chunksStored;

    /** Number of videos found (for playlists/channels) */
    private int videoCount;

    /** Total transcript segments */
    private int segmentCount;

    /** Title of the content (available after transcript extraction) */
    private String title;

    /** Error message if status is FAILED */
    private String errorMessage;

    /** When ingestion started */
    private Instant startedAt;

    /** When ingestion completed (null if still processing) */
    private Instant completedAt;

    /** Estimated time remaining in seconds (null if completed) */
    private Integer estimatedTimeRemainingSeconds;

    /**
     * Create a progress object for a new ingest starting.
     */
    public static IngestProgress started(UUID sessionId) {
        return IngestProgress.builder()
                .contentSessionId(sessionId)
                .status("PENDING")
                .currentStep("Queued for processing")
                .percentComplete(0)
                .totalChunks(0)
                .chunksEmbedded(0)
                .chunksStored(0)
                .videoCount(0)
                .segmentCount(0)
                .startedAt(Instant.now())
                .build();
    }

    /**
     * Mark as extracting transcript.
     */
    public IngestProgress withExtractingTranscript() {
        this.status = "EXTRACTING_TRANSCRIPT";
        this.currentStep = "Extracting transcript from YouTube...";
        this.percentComplete = 5;
        return this;
    }

    /**
     * Mark as chunking, with segment count.
     */
    public IngestProgress withChunking(int segmentCount, int videoCount) {
        this.status = "CHUNKING";
        this.currentStep = "Splitting transcript into chunks...";
        this.segmentCount = segmentCount;
        this.videoCount = videoCount;
        this.percentComplete = 15;
        return this;
    }

    /**
     * Mark as embedding, with total chunk count.
     */
    public IngestProgress withEmbedding(int totalChunks, String title) {
        this.status = "EMBEDDING";
        this.totalChunks = totalChunks;
        this.title = title;
        this.percentComplete = 20;
        return this;
    }

    /**
     * Update embedding progress for a single chunk.
     */
    public IngestProgress withChunkEmbedded() {
        this.chunksEmbedded++;
        this.currentStep = String.format("Embedding chunks... (%d/%d)", this.chunksEmbedded, this.totalChunks);
        // Embedding is 20% to 70% of total progress
        if (this.totalChunks > 0) {
            this.percentComplete = 20 + (int) ((double) this.chunksEmbedded / this.totalChunks * 50);
        }
        updateEstimatedTime();
        return this;
    }

    /**
     * Mark as storing.
     */
    public IngestProgress withStoring() {
        this.status = "STORING";
        this.currentStep = "Storing vectors in Pinecone...";
        this.percentComplete = 75;
        return this;
    }

    /**
     * Update storage progress for a single chunk.
     */
    public IngestProgress withChunkStored() {
        this.chunksStored++;
        this.currentStep = String.format("Storing vectors... (%d/%d)", this.chunksStored, this.totalChunks);
        // Storing is 75% to 95% of total progress
        if (this.totalChunks > 0) {
            this.percentComplete = 75 + (int) ((double) this.chunksStored / this.totalChunks * 20);
        }
        return this;
    }

    /**
     * Mark as completed.
     */
    public IngestProgress withCompleted() {
        this.status = "COMPLETED";
        this.currentStep = "Ingestion complete! Chat is ready.";
        this.percentComplete = 100;
        this.completedAt = Instant.now();
        this.estimatedTimeRemainingSeconds = null;
        return this;
    }

    /**
     * Mark as failed.
     */
    public IngestProgress withFailed(String errorMessage) {
        this.status = "FAILED";
        this.currentStep = "Ingestion failed";
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.estimatedTimeRemainingSeconds = null;
        return this;
    }

    /**
     * Estimate remaining time based on embedding speed (~1.7s per chunk).
     */
    private void updateEstimatedTime() {
        if ("EMBEDDING".equals(this.status) && this.totalChunks > 0) {
            int remaining = this.totalChunks - this.chunksEmbedded;
            // ~1.7s per chunk for embedding + ~0.2s per chunk for storing
            this.estimatedTimeRemainingSeconds = (int) (remaining * 1.9);
        } else if ("STORING".equals(this.status) && this.totalChunks > 0) {
            int remaining = this.totalChunks - this.chunksStored;
            this.estimatedTimeRemainingSeconds = (int) (remaining * 0.2);
        }
    }
}
