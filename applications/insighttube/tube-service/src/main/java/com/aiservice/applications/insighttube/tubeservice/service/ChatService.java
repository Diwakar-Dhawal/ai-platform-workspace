package com.aiservice.applications.insighttube.tubeservice.service;

import com.aiservice.applications.insighttube.tubeservice.client.AiPlatformClient;
import com.aiservice.applications.insighttube.tubeservice.dto.ChatRequest;
import com.aiservice.applications.insighttube.tubeservice.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Chat service for InsightTube.
 * Delegates RAG completion to AI Platform with the "insighttube" appId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AiPlatformClient aiPlatformClient;
    private final IngestionService ingestionService;

    /**
     * Chat about ingested content.
     * If contentSessionId is provided, searches within that session.
     * Otherwise, searches across all InsightTube content.
     */
    public ChatResponse chat(ChatRequest request, UUID userId) {
        String appId = "insighttube";

        // Validate content session exists if provided
        if (request.getContentSessionId() != null) {
            IngestionService.ContentSession session = ingestionService.getSession(request.getContentSessionId());
            if (session == null) {
                throw new IllegalArgumentException("Content session not found: " + request.getContentSessionId());
            }
        }

        log.info("Chat request: appId={}, sessionId={}, message='{}'",
                appId, request.getContentSessionId(),
                request.getMessage().length() > 50
                        ? request.getMessage().substring(0, 50) + "..."
                        : request.getMessage());

        // Call AI Platform for RAG completion
        String answer = aiPlatformClient.chat(
                request.getMessage(),
                appId,
                request.getContentSessionId()
        );

        return ChatResponse.builder()
                .answer(answer)
                .sources(List.of())  // Sources come from AI Platform response
                .model("gemini-3-flash-preview")
                .build();
    }
}
