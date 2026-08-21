package com.aiservice.platform.aiplatform.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchResult Model")
class SearchResultTest {

    @Test
    @DisplayName("should create search result with chunk and score")
    void shouldCreateSearchResult() {
        Chunk chunk = Chunk.builder()
                .id(UUID.randomUUID())
                .sessionId(UUID.randomUUID())
                .appId("insighttube")
                .content("The quick brown fox jumps over the lazy dog")
                .chunkIndex(0)
                .totalChunks(1)
                .metadata(Map.of("timestamp", "0:45"))
                .build();

        SearchResult result = SearchResult.builder()
                .chunk(chunk)
                .score(0.92)
                .citation("Video at 0:45")
                .build();

        assertEquals(0.92, result.getScore(), 0.001);
        assertEquals("Video at 0:45", result.getCitation());
        assertEquals("insighttube", result.getChunk().getAppId());
    }

    @Test
    @DisplayName("should handle zero score")
    void shouldHandleZeroScore() {
        SearchResult result = SearchResult.builder()
                .chunk(Chunk.builder().content("test").build())
                .score(0.0)
                .citation("No match")
                .build();

        assertEquals(0.0, result.getScore());
    }
}
