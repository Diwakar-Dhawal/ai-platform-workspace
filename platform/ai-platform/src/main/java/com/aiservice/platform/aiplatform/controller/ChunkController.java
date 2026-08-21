package com.aiservice.platform.aiplatform.controller;

import com.aiservice.platform.aiplatform.model.Chunk;
import com.aiservice.platform.aiplatform.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/chunks")
@RequiredArgsConstructor
public class ChunkController {

    private final VectorStoreService vectorStoreService;

    @PostMapping("/store")
    public Map<String, String> store(@RequestBody Map<String, Object> request) {
        UUID chunkId = UUID.fromString((String) request.get("id"));
        UUID sessionId = UUID.fromString((String) request.get("sessionId"));
        String appId = (String) request.get("appId");
        String content = (String) request.get("content");
        int chunkIndex = (int) request.get("chunkIndex");
        int totalChunks = (int) request.get("totalChunks");

        @SuppressWarnings("unchecked")
        List<Number> embeddingNumbers = (List<Number>) request.get("embedding");
        List<Float> embedding = embeddingNumbers.stream()
                .map(Number::floatValue)
                .toList();

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) request.getOrDefault("metadata", Map.of());

        log.info("Store chunk {}/{} in session {} (appId={}, content={} chars)",
                chunkIndex + 1, totalChunks, sessionId, appId, content.length());

        Chunk chunk = Chunk.builder()
                .id(chunkId)
                .sessionId(sessionId)
                .appId(appId)
                .content(content)
                .chunkIndex(chunkIndex)
                .totalChunks(totalChunks)
                .metadata(metadata)
                .build();

        vectorStoreService.store(chunk, embedding);

        return Map.of("status", "stored", "chunkId", chunkId.toString());
    }
}
