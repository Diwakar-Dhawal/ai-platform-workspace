package com.aiservice.applications.insighttube.tubeservice.service;

import com.aiservice.applications.insighttube.tubeservice.client.AiPlatformClient;
import com.aiservice.applications.insighttube.tubeservice.client.TranscriptClient;
import com.aiservice.applications.insighttube.tubeservice.dto.IngestProgress;
import com.aiservice.applications.insighttube.tubeservice.dto.IngestRequest;
import com.aiservice.applications.insighttube.tubeservice.dto.IngestResponse;
import com.aiservice.applications.insighttube.tubeservice.dto.TranscriptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the full content ingestion pipeline:
 *   YouTube URL → Transcript → Chunk → Embed → Store in Pinecone
 *
 * Ingestion runs asynchronously. Frontend polls GET /sessions/{id}/status
 * for real-time progress updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final TranscriptClient transcriptClient;
    private final AiPlatformClient aiPlatformClient;
    private final TranscriptChunker chunker;

    /** In-memory session and progress stores (replace with DB in production) */
    private final Map<UUID, ContentSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, IngestProgress> progressMap = new ConcurrentHashMap<>();

    public record ContentSession(
            UUID id,
            String title,
            String sourceUrl,
            String contentType,
            int chunkCount,
            int videoCount,
            String status,
            UUID userId
    ) {}

    /**
     * Start ingestion asynchronously. Returns immediately with sessionId.
     * Frontend polls GET /sessions/{id}/status for progress.
     */
    public IngestResponse ingest(IngestRequest request, UUID userId) {
        UUID sessionId = UUID.randomUUID();
        log.info("Starting ingestion for URL: {} (session: {})", request.getUrl(), sessionId);

        // Initialize progress
        IngestProgress progress = IngestProgress.started(sessionId);
        progressMap.put(sessionId, progress);

        // Save placeholder session immediately
        ContentSession placeholder = new ContentSession(
                sessionId, "Processing...", request.getUrl(), "unknown",
                0, 0, "processing", userId
        );
        sessions.put(sessionId, placeholder);

        // Run pipeline asynchronously
        runPipeline(sessionId, request, userId);

        // Return immediately — frontend polls for progress
        return IngestResponse.builder()
                .status("processing")
                .contentSessionId(sessionId)
                .title("Processing...")
                .sourceUrl(request.getUrl())
                .chunkCount(0)
                .message("Ingestion started. Poll GET /api/v1/content/sessions/" + sessionId + "/status for progress.")
                .build();
    }

    /**
     * The actual pipeline, runs in a separate thread.
     */
    @Async
    public void runPipeline(UUID sessionId, IngestRequest request, UUID userId) {
        IngestProgress progress = progressMap.get(sessionId);

        try {
            // ── Step 1: Extract transcript ──
            log.info("Step 1: Extracting transcript for session {}", sessionId);
            progress.withExtractingTranscript();

            TranscriptResponse transcript = transcriptClient.extractTranscript(
                    request.getUrl(),
                    request.getLanguages()
            );

            if (transcript.getVideos() == null || transcript.getVideos().isEmpty()) {
                throw new RuntimeException("No transcripts found for URL: " + request.getUrl());
            }

            String title = transcript.getVideos().get(0).getTitle();
            log.info("Extracted {} videos, {} segments (title: {})",
                    transcript.getTotalVideos(), transcript.getTotalSegments(), title);

            // ── Step 2: Chunk transcripts ──
            log.info("Step 2: Chunking transcripts for session {}", sessionId);
            progress.withChunking(transcript.getTotalSegments(), transcript.getTotalVideos());

            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(
                    transcript.getVideos(),
                    sessionId
            );
            log.info("Created {} chunks", chunks.size());

            // ── Step 3: Embed and store chunks ──
            log.info("Step 3: Embedding and storing {} chunks for session {}", chunks.size(), sessionId);
            progress.withEmbedding(chunks.size(), title);

            String appId = "insighttube";
            int storedCount = 0;

            for (TranscriptChunker.ChunkData chunk : chunks) {
                try {
                    // Embed via AI Platform
                    List<Float> embedding = aiPlatformClient.embed(chunk.content());
                    progress.withChunkEmbedded();

                    // Store via AI Platform
                    aiPlatformClient.storeChunk(
                            chunk.chunkId(),
                            sessionId,
                            appId,
                            chunk.content(),
                            chunk.chunkIndex(),
                            chunk.totalChunks(),
                            embedding,
                            chunk.metadata()
                    );
                    progress.withChunkStored();

                    storedCount++;
                } catch (Exception e) {
                    log.warn("Failed to store chunk {}: {}", chunk.chunkId(), e.getMessage());
                    // Still count as embedded, just failed to store
                    progress.withChunkStored();
                }
            }

            log.info("Stored {}/{} chunks for session {}", storedCount, chunks.size(), sessionId);

            // ── Step 4: Mark completed ──
            progress.withCompleted();

            ContentSession completedSession = new ContentSession(
                    sessionId,
                    title,
                    request.getUrl(),
                    transcript.getContentType(),
                    storedCount,
                    transcript.getTotalVideos(),
                    "completed",
                    userId
            );
            sessions.put(sessionId, completedSession);

            log.info("Ingestion completed for session {} ({} chunks, {} videos)",
                    sessionId, storedCount, transcript.getTotalVideos());

        } catch (Exception e) {
            log.error("Ingestion failed for session {} (URL: {})", sessionId, request.getUrl(), e);
            progress.withFailed(e.getMessage());

            ContentSession failedSession = new ContentSession(
                    sessionId,
                    "Failed",
                    request.getUrl(),
                    "unknown",
                    0,
                    0,
                    "failed",
                    userId
            );
            sessions.put(sessionId, failedSession);
        }
    }

    /**
     * Get real-time progress for an ingestion session.
     * This is what the frontend polls every 2 seconds.
     */
    public IngestProgress getProgress(UUID sessionId) {
        IngestProgress progress = progressMap.get(sessionId);
        if (progress == null) {
            // Check if session exists but progress expired
            ContentSession session = sessions.get(sessionId);
            if (session == null) {
                return null;
            }
            // Return a basic progress from session status
            return IngestProgress.builder()
                    .contentSessionId(sessionId)
                    .status(session.status().toUpperCase())
                    .currentStep(session.status())
                    .percentComplete("completed".equals(session.status()) ? 100 : 0)
                    .title(session.title())
                    .build();
        }
        return progress;
    }

    /**
     * Get a content session by ID.
     */
    public ContentSession getSession(UUID sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * List all sessions for a user.
     */
    public List<ContentSession> listSessions(UUID userId) {
        return sessions.values().stream()
                .filter(s -> s.userId().equals(userId))
                .toList();
    }

    /**
     * Replace a content session (e.g., for rename).
     */
    public void updateSession(UUID sessionId, ContentSession updated) {
        sessions.put(sessionId, updated);
    }

    /**
     * Delete a content session and its progress.
     */
    public void deleteSession(UUID sessionId) {
        sessions.remove(sessionId);
        progressMap.remove(sessionId);
        log.info("Deleted session {} and its progress", sessionId);
    }
}
