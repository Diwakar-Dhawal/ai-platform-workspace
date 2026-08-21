package com.aiservice.platform.aiplatform.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chunk Model")
class ChunkTest {

    @Test
    @DisplayName("should create chunk with builder")
    void shouldCreateChunkWithBuilder() {
        UUID id = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Chunk chunk = Chunk.builder()
                .id(id)
                .sessionId(sessionId)
                .appId("insighttube")
                .content("Test content for embedding")
                .chunkIndex(0)
                .totalChunks(5)
                .metadata(Map.of("videoId", "abc123", "timestamp", "1:23"))
                .build();

        assertEquals(id, chunk.getId());
        assertEquals(sessionId, chunk.getSessionId());
        assertEquals("insighttube", chunk.getAppId());
        assertEquals("Test content for embedding", chunk.getContent());
        assertEquals(0, chunk.getChunkIndex());
        assertEquals(5, chunk.getTotalChunks());
        assertEquals("abc123", chunk.getMetadata().get("videoId"));
        assertNotNull(chunk.getCreatedAt());
    }

    @Test
    @DisplayName("should create chunk with default values")
    void shouldCreateChunkWithDefaults() {
        Chunk chunk = Chunk.builder()
                .content("Simple content")
                .build();

        assertNotNull(chunk.getId());
        assertNotNull(chunk.getCreatedAt());
        assertEquals("Simple content", chunk.getContent());
    }
}
