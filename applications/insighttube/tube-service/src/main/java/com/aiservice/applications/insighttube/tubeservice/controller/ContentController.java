package com.aiservice.applications.insighttube.tubeservice.controller;

import com.aiservice.applications.insighttube.tubeservice.dto.*;
import com.aiservice.applications.insighttube.tubeservice.service.ChatService;
import com.aiservice.applications.insighttube.tubeservice.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        log.info("Ingest request from user {}: {}", userId, request.getUrl());
        return ingestionService.ingest(request, userId);
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat about content", description = "Ask questions about ingested YouTube content using RAG")
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return chatService.chat(request, userId);
    }

    @GetMapping("/sessions")
    @Operation(summary = "List content sessions", description = "Get all ingested content sessions for the current user")
    public List<IngestionService.ContentSession> listSessions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ingestionService.listSessions(userId);
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
}
