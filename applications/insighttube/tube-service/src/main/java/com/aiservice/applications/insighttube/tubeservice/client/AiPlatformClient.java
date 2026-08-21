package com.aiservice.applications.insighttube.tubeservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calls AI Platform for embedding, vector storage, and LLM completions.
 */
@Slf4j
@Component
public class AiPlatformClient {

    @Value("${services.ai-platform.url}")
    private String aiPlatformUrl;

    @Value("${services.ai-platform-secret:}")
    private String serviceSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Store a chunk with its embedding in the vector store.
     */
    public void storeChunk(UUID chunkId, UUID sessionId, String appId, String content,
                            int chunkIndex, int totalChunks, List<Float> embedding,
                            Map<String, Object> metadata) {
        try {
            Map<String, Object> body = Map.of(
                    "id", chunkId.toString(),
                    "sessionId", sessionId.toString(),
                    "appId", appId,
                    "content", content,
                    "chunkIndex", chunkIndex,
                    "totalChunks", totalChunks,
                    "embedding", embedding,
                    "metadata", metadata != null ? metadata : Map.of()
            );

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            restTemplate.postForObject(
                    aiPlatformUrl + "/api/v1/chunks/store",
                    request,
                    String.class
            );

            log.debug("Stored chunk {} ({}/{})", chunkId, chunkIndex + 1, totalChunks);

        } catch (Exception e) {
            log.error("Failed to store chunk {}: {}", chunkId, e.getMessage());
            throw new RuntimeException("Chunk storage failed: " + e.getMessage(), e);
        }
    }

    /**
     * Embed text and return the embedding vector.
     */
    public List<Float> embed(String text) {
        try {
            Map<String, Object> body = Map.of("text", text);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiPlatformUrl + "/api/v1/embeddings/embed",
                    request,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("embedding");
            List<Float> embedding = new java.util.ArrayList<>();
            values.forEach(node -> embedding.add((float) node.asDouble()));

            return embedding;

        } catch (Exception e) {
            log.error("Failed to embed text: {}", e.getMessage());
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Send a chat completion request to AI Platform.
     */
    public String chat(String message, String appId, UUID contentSessionId) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("appId", appId);
            body.put("message", message);
            if (contentSessionId != null) {
                body.put("contentSessionId", contentSessionId.toString());
            }

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiPlatformUrl + "/api/v1/completions/chat",
                    request,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("answer").asText();

        } catch (Exception e) {
            log.error("Chat completion failed: {}", e.getMessage());
            throw new RuntimeException("Chat failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (serviceSecret != null && !serviceSecret.isBlank()) {
            headers.set("X-Service-Secret", serviceSecret);
        }
        return headers;
    }
}
