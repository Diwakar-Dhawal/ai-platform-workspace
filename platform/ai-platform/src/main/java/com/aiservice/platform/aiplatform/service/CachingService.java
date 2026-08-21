package com.aiservice.platform.aiplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis caching layer for the RAG pipeline.
 *
 * Cache strategy:
 *   - Embeddings: cache for 24 hours (same question = same embedding)
 *   - Search results: cache for 1 hour (same query + same content = same results)
 *   - Completions: cache for 1 hour (same question + same content = same answer)
 *   - Transcripts: cache for 7 days (videos don't change) — handled by Python service
 */
@Slf4j
@Service
public class CachingService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String EMBEDDING_PREFIX = "emb:";
    private static final String SEARCH_PREFIX = "search:";
    private static final String COMPLETION_PREFIX = "completion:";

    private static final long EMBEDDING_TTL_HOURS = 24;
    private static final long SEARCH_TTL_HOURS = 1;
    private static final long COMPLETION_TTL_HOURS = 1;

    public CachingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // ─── Embedding Cache ────────────────────────────────────────────────

    public Optional<List<Float>> getCachedEmbedding(String text) {
        try {
            String cached = redisTemplate.opsForValue().get(EMBEDDING_PREFIX + hash(text));
            if (cached != null) {
                log.debug("Cache HIT for embedding");
                return Optional.of(objectMapper.readValue(cached, new TypeReference<>() {}));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached embedding: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void cacheEmbedding(String text, List<Float> embedding) {
        try {
            String json = objectMapper.writeValueAsString(embedding);
            redisTemplate.opsForValue().set(
                    EMBEDDING_PREFIX + hash(text),
                    json,
                    EMBEDDING_TTL_HOURS,
                    TimeUnit.HOURS
            );
            log.debug("Cached embedding for text ({} chars)", text.length());
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache embedding: {}", e.getMessage());
        }
    }

    // ─── Search Result Cache ────────────────────────────────────────────

    public Optional<String> getCachedSearch(String query, String appId, UUID contentSessionId) {
        String key = SEARCH_PREFIX + hash(query + ":" + appId + ":" + contentSessionId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache HIT for search results");
            return Optional.of(cached);
        }
        return Optional.empty();
    }

    public void cacheSearch(String query, String appId, UUID contentSessionId, String resultsJson) {
        String key = SEARCH_PREFIX + hash(query + ":" + appId + ":" + contentSessionId);
        redisTemplate.opsForValue().set(key, resultsJson, SEARCH_TTL_HOURS, TimeUnit.HOURS);
    }

    // ─── Completion Cache ───────────────────────────────────────────────

    public Optional<String> getCachedCompletion(String query, String appId, UUID contentSessionId) {
        String key = COMPLETION_PREFIX + hash(query + ":" + appId + ":" + contentSessionId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache HIT for completion");
            return Optional.of(cached);
        }
        return Optional.empty();
    }

    public void cacheCompletion(String query, String appId, UUID contentSessionId, String completion) {
        String key = COMPLETION_PREFIX + hash(query + ":" + appId + ":" + contentSessionId);
        redisTemplate.opsForValue().set(key, completion, COMPLETION_TTL_HOURS, TimeUnit.HOURS);
    }

    // ─── Utility ────────────────────────────────────────────────────────

    private String hash(String input) {
        return Integer.toHexString(input.hashCode());
    }

    public boolean isAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
