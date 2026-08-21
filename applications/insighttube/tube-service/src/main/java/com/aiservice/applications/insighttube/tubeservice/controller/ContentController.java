package com.aiservice.applications.insighttube.tubeservice.controller;

import com.aiservice.applications.insighttube.tubeservice.dto.*;
import com.aiservice.applications.insighttube.tubeservice.service.ChatService;
import com.aiservice.applications.insighttube.tubeservice.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Tag(name = "Content", description = "YouTube content ingestion and chat")
public class ContentController {

    private final IngestionService ingestionService;
    private final ChatService chatService;

    @PostMapping("/ingest")
    @Operation(summary = "Ingest YouTube content", description = "Extract transcript, chunk, embed, and store in vector database")
    public IngestResponse ingest(
            @Valid @RequestBody IngestRequest request,
            @Nullable Authentication authentication
    ) {
        UUID userId = getUserId(authentication);
        log.info("Ingest request from user {}: {}", userId, request.getUrl());
        return ingestionService.ingest(request, userId);
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat about content", description = "Ask questions about ingested YouTube content using RAG")
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request,
            @Nullable Authentication authentication
    ) {
        UUID userId = getUserId(authentication);
        return chatService.chat(request, userId);
    }

    @GetMapping("/sessions")
    @Operation(summary = "List content sessions", description = "Get all ingested content sessions for the current user")
    public List<IngestionService.ContentSession> listSessions(@Nullable Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ingestionService.listSessions(userId);
    }

    /**
     * Extract user ID from JWT. Falls back to a dev user UUID when unauthenticated (dev mode).
     */
    private UUID getUserId(@Nullable Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            return UUID.fromString(authentication.getName());
        }
        // Dev mode: return a fixed user ID so unauthenticated requests work
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get content session", description = "Get details of a specific content session")
    public IngestionService.ContentSession getSession(@PathVariable UUID sessionId) {
        IngestionService.ContentSession session = ingestionService.getSession(sessionId);
        if (session == null) {
            throw new jakarta.persistence.EntityNotFoundException("Session not found: " + sessionId);
        }
        return session;
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(
            summary = "Get ingest progress",
            description = "Poll this endpoint every 2 seconds during ingestion to track real-time progress. " +
                    "Returns status, percent complete, chunks embedded/stored, and estimated time remaining."
    )
    public IngestProgress getIngestProgress(@PathVariable UUID sessionId) {
        IngestProgress progress = ingestionService.getProgress(sessionId);
        if (progress == null) {
            throw new jakarta.persistence.EntityNotFoundException("Session not found: " + sessionId);
        }
        log.debug("Progress for session {}: {} ({}%)", sessionId, progress.getStatus(), progress.getPercentComplete());
        return progress;
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete content session", description = "Remove ingested content and its vectors")
    public Map<String, String> deleteSession(@PathVariable UUID sessionId) {
        IngestionService.ContentSession session = ingestionService.getSession(sessionId);
        if (session == null) {
            throw new jakarta.persistence.EntityNotFoundException("Session not found: " + sessionId);
        }
        ingestionService.deleteSession(sessionId);
        return Map.of("status", "deleted", "sessionId", sessionId.toString());
    }
}
