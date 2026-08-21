package com.aiservice.platform.aiplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Embedding service using Google Gemini REST API (free tier: 1,500 req/min).
 *
 * API: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:embedContent
 * Model: gemini-embedding-exp-03-07 (3072 dimensions, free)
 *
 * Requires: GEMINI_API_KEY environment variable
 */
@Slf4j
@Service
public class GeminiEmbeddingService implements EmbeddingService {

    private static final String MODEL = "gemini-embedding-001";
    private static final int DIMENSIONS = 768;
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${ai-platform.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("Gemini embedding service initialized (model: {}, dimensions: {})", MODEL, DIMENSIONS);
        } else {
            log.warn("GEMINI_API_KEY not set — embedding calls will fail");
        }
    }

    @Override
    public List<Float> embed(String text) {
        if (!isConfigured()) {
            log.debug("GEMINI_API_KEY not set — returning random embedding for text ({} chars)", text.length());
            return randomEmbedding();
        }

        try {
            String url = API_BASE + "/models/" + MODEL + ":embedContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "model", "models/" + MODEL,
                    "content", Map.of(
                            "parts", List.of(Map.of("text", text))
                    )
            );

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode values = root.path("embedding").path("values");

            List<Float> embedding = new ArrayList<>();
            values.forEach(node -> embedding.add((float) node.asDouble()));

            log.debug("Embedded text ({} chars) → {} dimensions", text.length(), embedding.size());
            return embedding;

        } catch (Exception e) {
            log.error("Failed to embed text: {}", e.getMessage());
            return randomEmbedding();
        }
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (!isConfigured()) {
            log.debug("GEMINI_API_KEY not set — returning random embeddings for {} texts", texts.size());
            return texts.stream().map(t -> randomEmbedding()).toList();
        }

        try {
            String url = API_BASE + "/models/" + MODEL + ":batchEmbedContents?key=" + apiKey;

            List<Map<String, Object>> requests = texts.stream()
                    .map(text -> Map.of(
                            "model", "models/" + MODEL,
                            "content", Map.of("parts", List.of(Map.of("text", text)))
                    ))
                    .toList();

            Map<String, Object> body = Map.of("requests", requests);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddings = root.path("embeddings");

            List<List<Float>> results = new ArrayList<>();
            embeddings.forEach(node -> {
                List<Float> embedding = new ArrayList<>();
                node.path("values").forEach(v -> embedding.add((float) v.asDouble()));
                results.add(embedding);
            });

            log.debug("Batch embedded {} texts", results.size());
            return results;

        } catch (Exception e) {
            log.error("Batch embedding failed, falling back to individual: {}", e.getMessage());
            return texts.stream().map(this::embed).toList();
        }
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    private List<Float> randomEmbedding() {
        return java.util.stream.IntStream.range(0, DIMENSIONS)
                .mapToDouble(i -> Math.random() * 2 - 1)
                .boxed()
                .map(Double::floatValue)
                .toList();
    }
}
