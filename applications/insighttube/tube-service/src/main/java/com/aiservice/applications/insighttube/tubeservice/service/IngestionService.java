package com.aiservice.applications.insighttube.tubeservice.service;

import com.aiservice.applications.insighttube.tubeservice.client.AiPlatformClient;
import com.aiservice.applications.insighttube.tubeservice.client.TranscriptClient;
import com.aiservice.applications.insighttube.tubeservice.dto.IngestRequest;
import com.aiservice.applications.insighttube.tubeservice.dto.IngestResponse;
import com.aiservice.applications.insighttube.tubeservice.dto.TranscriptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the full content ingestion pipeline:
 *   YouTube URL → Transcript → Chunk → Embed → Store in Pinecone
 *
 * Content sessions are stored in-memory for MVP.
 * Replace with database in production.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final TranscriptClient transcriptClient;
    private final AiPlatformClient aiPlatformClient;
    private final TranscriptChunker chunker;

    /** In-memory session store (replace with DB in production) */
    private final Map<UUID, ContentSession> sessions = new ConcurrentHashMap<>();

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
     * Full ingestion pipeline: extract → chunk → embed → store.
     */
    public IngestResponse ingest(IngestRequest request, UUID userId) {
        UUID sessionId = UUID.randomUUID();
        log.info("Starting ingestion for URL: {} (session: {})", request.getUrl(), sessionId);

        try {
            // 1. Extract transcript from Python service
            log.info("Step 1: Extracting transcript...");
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

            // 2. Chunk the transcripts
            log.info("Step 2: Chunking transcripts...");
            List<TranscriptChunker.ChunkData> chunks = chunker.chunk(
                    transcript.getVideos(),
                    sessionId
            );
            log.info("Created {} chunks", chunks.size());

            // 3. Embed and store each chunk via AI Platform
            log.info("Step 3: Embedding and storing chunks...");
            String appId = "insighttube";
            int storedCount = 0;

            for (TranscriptChunker.ChunkData chunk : chunks) {
                try {
                    // Embed via AI Platform
                    List<Float> embedding = aiPlatformClient.embed(chunk.content());

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

                    storedCount++;
                } catch (Exception e) {
                    log.warn("Failed to store chunk {}: {}", chunk.chunkId(), e.getMessage());
                    // Continue with other chunks
                }
            }

            log.info("Stored {}/{} chunks", storedCount, chunks.size());

            // 4. Save content session
            ContentSession session = new ContentSession(
                    sessionId,
                    title,
                    request.getUrl(),
                    transcript.getContentType(),
                    storedCount,
                    transcript.getTotalVideos(),
                    "completed",
                    userId
            );
            sessions.put(sessionId, session);

            // 5. Return response
            return IngestResponse.builder()
                    .status("success")
                    .contentSessionId(sessionId)
                    .title(title)
                    .sourceUrl(request.getUrl())
                    .chunkCount(storedCount)
                    .message(String.format("Ingested %d videos into %d chunks", transcript.getTotalVideos(), storedCount))
                    .build();

        } catch (Exception e) {
            log.error("Ingestion failed for URL: {}", request.getUrl(), e);

            // Save failed session
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

            throw new RuntimeException("Ingestion failed: " + e.getMessage(), e);
        }
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
}
