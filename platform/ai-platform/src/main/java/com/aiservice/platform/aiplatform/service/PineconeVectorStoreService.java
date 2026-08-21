package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.model.Chunk;
import com.aiservice.platform.aiplatform.model.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vector store using Pinecone REST API (free tier: 100K vectors, 1GB).
 *
 * No SDK dependency — uses Pinecone's REST API directly.
 * Index structure:
 *   - Namespace per application (insighttube, pdfmind, etc.)
 *   - Vector ID: chunk ID
 *   - Metadata: content, sessionId, chunkIndex, appId, + app-specific fields
 *
 * Requires: PINECONE_API_KEY and PINECONE_INDEX_HOST environment variables
 */
@Slf4j
@Service
public class PineconeVectorStoreService implements VectorStoreService {

    @Value("${ai-platform.pinecone.api-key:${PINECONE_API_KEY:}}")
    private String apiKey;

    @Value("${ai-platform.pinecone.index-host:${PINECONE_INDEX_HOST:}}")
    private String indexHost;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        if (apiKey != null && !apiKey.isBlank() && indexHost != null && !indexHost.isBlank()) {
            log.info("Pinecone REST client initialized for index: {}", indexHost);
        } else {
            log.warn("PINECONE_API_KEY or PINECONE_INDEX_HOST not set — vector store calls will fail");
        }
    }

    @Override
    public void store(Chunk chunk, List<Float> embedding) {
        ensureConfigured();

        try {
            Map<String, Object> vector = buildVector(chunk, embedding);
            Map<String, Object> body = Map.of("vectors", List.of(vector), "namespace", chunk.getAppId());

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            restTemplate.postForObject(
                    "https://" + indexHost + "/vectors/upsert",
                    request,
                    String.class
            );

            log.debug("Stored chunk {} in namespace '{}'", chunk.getId(), chunk.getAppId());

        } catch (Exception e) {
            log.error("Failed to store chunk {}: {}", chunk.getId(), e.getMessage());
            throw new RuntimeException("Vector store failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void storeBatch(List<ChunkWithEmbedding> chunks) {
        ensureConfigured();

        try {
            // Group by namespace
            Map<String, List<Map<String, Object>>> byNamespace = new HashMap<>();

            for (ChunkWithEmbedding item : chunks) {
                Map<String, Object> vector = buildVector(item.chunk(), item.embedding());
                byNamespace.computeIfAbsent(item.chunk().getAppId(), k -> new ArrayList<>())
                        .add(vector);
            }

            for (Map.Entry<String, List<Map<String, Object>>> entry : byNamespace.entrySet()) {
                Map<String, Object> body = Map.of("vectors", entry.getValue(), "namespace", entry.getKey());

                HttpEntity<String> request = new HttpEntity<>(
                        objectMapper.writeValueAsString(body),
                        createHeaders()
                );

                restTemplate.postForObject(
                        "https://" + indexHost + "/vectors/upsert",
                        request,
                        String.class
                );

                log.info("Batch stored {} chunks in namespace '{}'", entry.getValue().size(), entry.getKey());
            }

        } catch (Exception e) {
            log.error("Batch store failed: {}", e.getMessage());
            throw new RuntimeException("Vector batch store failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SearchResult> search(
            List<Float> queryEmbedding,
            String appId,
            int topK,
            double similarityThreshold
    ) {
        ensureConfigured();

        try {
            Map<String, Object> body = Map.of(
                    "vector", queryEmbedding,
                    "topK", topK,
                    "namespace", appId,
                    "includeMetadata", true
            );

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://" + indexHost + "/query",
                    request,
                    String.class
            );

            return parseSearchResults(response.getBody());

        } catch (Exception e) {
            log.error("Search failed in namespace '{}': {}", appId, e.getMessage());
            throw new RuntimeException("Vector search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SearchResult> searchWithinSession(
            List<Float> queryEmbedding,
            UUID contentSessionId,
            int topK,
            double similarityThreshold
    ) {
        ensureConfigured();

        try {
            // Over-fetch then filter client-side (Pinecone metadata filtering requires paid plan)
            Map<String, Object> body = Map.of(
                    "vector", queryEmbedding,
                    "topK", topK * 3,
                    "includeMetadata", true
            );

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://" + indexHost + "/query",
                    request,
                    String.class
            );

            List<SearchResult> allResults = parseSearchResults(response.getBody());

            // Filter by sessionId
            return allResults.stream()
                    .filter(r -> {
                        String sid = String.valueOf(r.getChunk().getMetadata().get("sessionId"));
                        return sid != null && sid.equals(contentSessionId.toString());
                    })
                    .limit(topK)
                    .toList();

        } catch (Exception e) {
            log.error("Session search failed: {}", e.getMessage());
            throw new RuntimeException("Vector search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteSession(UUID contentSessionId) {
        log.warn("deleteSession({}) — metadata-based delete requires Pinecone paid plan", contentSessionId);
    }

    @Override
    public void deleteApp(String appId) {
        ensureConfigured();

        try {
            Map<String, Object> body = Map.of("deleteAll", true, "namespace", appId);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            restTemplate.postForObject(
                    "https://" + indexHost + "/vectors/delete",
                    request,
                    String.class
            );

            log.info("Deleted all vectors in namespace '{}'", appId);
        } catch (Exception e) {
            log.error("Failed to delete namespace '{}': {}", appId, e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Map<String, Object> buildVector(Chunk chunk, List<Float> embedding) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", chunk.getContent());
        metadata.put("sessionId", chunk.getSessionId().toString());
        metadata.put("appId", chunk.getAppId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("totalChunks", chunk.getTotalChunks());

        if (chunk.getMetadata() != null) {
            metadata.putAll(chunk.getMetadata());
        }

        return Map.of(
                "id", chunk.getId().toString(),
                "values", embedding,
                "metadata", metadata
        );
    }

    private List<SearchResult> parseSearchResults(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode matches = root.get("matches");
            List<SearchResult> results = new ArrayList<>();

            if (matches != null) {
                for (JsonNode match : matches) {
                    String id = match.get("id").asText();
                    double score = match.get("score").asDouble();
                    JsonNode metadata = match.get("metadata");

                    String content = metadata.has("content") ? metadata.get("content").asText() : "";
                    String sessionId = metadata.has("sessionId") ? metadata.get("sessionId").asText() : "";
                    String appId = metadata.has("appId") ? metadata.get("appId").asText() : "";
                    int chunkIndex = metadata.has("chunkIndex") ? metadata.get("chunkIndex").asInt() : 0;
                    int totalChunks = metadata.has("totalChunks") ? metadata.get("totalChunks").asInt() : 1;

                    Map<String, Object> metaMap = new HashMap<>();
                    metadata.fields().forEachRemaining(e -> metaMap.put(e.getKey(), e.getValue().asText()));

                    Chunk chunk = Chunk.builder()
                            .id(UUID.fromString(id))
                            .sessionId(UUID.fromString(sessionId))
                            .appId(appId)
                            .content(content)
                            .chunkIndex(chunkIndex)
                            .totalChunks(totalChunks)
                            .metadata(metaMap)
                            .build();

                    String citation = buildCitation(chunk);

                    results.add(SearchResult.builder()
                            .chunk(chunk)
                            .score(score)
                            .citation(citation)
                            .build());
                }
            }

            return results;

        } catch (Exception e) {
            log.error("Failed to parse Pinecone response: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildCitation(Chunk chunk) {
        Map<String, Object> meta = chunk.getMetadata();
        if (meta.containsKey("timestamp")) {
            return "Video at " + meta.get("timestamp");
        }
        if (meta.containsKey("page_number")) {
            return "Page " + meta.get("page_number");
        }
        if (meta.containsKey("section_title")) {
            return String.valueOf(meta.get("section_title"));
        }
        return "Chunk " + chunk.getChunkIndex();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        return headers;
    }

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank() || indexHost == null || indexHost.isBlank()) {
            throw new IllegalStateException("Pinecone not configured — check PINECONE_API_KEY and PINECONE_INDEX_HOST");
        }
    }
}
