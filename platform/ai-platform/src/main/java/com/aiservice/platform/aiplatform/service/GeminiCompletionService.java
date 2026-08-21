package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.config.PromptConfig;
import com.aiservice.platform.aiplatform.model.CompletionRequest;
import com.aiservice.platform.aiplatform.model.CompletionResponse;
import com.aiservice.platform.aiplatform.model.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Completion service using Google Gemini REST API (free tier: 15 RPM, 1M tokens/day).
 *
 * API: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 * Model: gemini-2.0-flash (fast, free, high quality)
 *
 * Pipeline:
 *   1. Check completion cache (Redis)
 *   2. Retrieve relevant context via RAG (Pinecone)
 *   3. Assemble system prompt + context + user query
 *   4. Call Gemini REST API
 *   5. Cache and return response
 *
 * Requires: GEMINI_API_KEY environment variable
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiCompletionService implements CompletionService {

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final RetrievalService retrievalService;
    private final PromptRegistry promptRegistry;
    private final CachingService cachingService;

    @Value("${ai-platform.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("Gemini completion service initialized");
        } else {
            log.warn("GEMINI_API_KEY not set — completion calls will fail");
        }
    }

    @Override
    public CompletionResponse complete(CompletionRequest request) {
        String appId = request.getAppId();

        // 1. Validate app is registered
        if (!promptRegistry.isRegistered(appId)) {
            throw new IllegalArgumentException("App not registered: " + appId);
        }

        PromptConfig.AppPromptConfig config = promptRegistry.getConfig(appId);

        // 2. Check completion cache
        UUID sessionId = request.getContentSessionId() != null
                ? request.getContentSessionId()
                : request.getSessionId();

        Optional<String> cachedCompletion = cachingService.getCachedCompletion(
                request.getMessage(), appId, sessionId
        );
        if (cachedCompletion.isPresent()) {
            log.info("Returning cached completion for app '{}'", appId);
            return CompletionResponse.builder()
                    .answer(cachedCompletion.get())
                    .sources(List.of())
                    .model("cached")
                    .tokensUsed(0)
                    .appId(appId)
                    .build();
        }

        // 3. Retrieve context via RAG
        int topK = request.getTopK() != null ? request.getTopK() : config.getTopK();
        List<SearchResult> searchResults;

        if (request.getContentSessionId() != null) {
            searchResults = retrievalService.retrieveWithinSession(
                    request.getMessage(), request.getContentSessionId(), topK
            );
        } else {
            searchResults = retrievalService.retrieve(request.getMessage(), appId, topK);
        }

        // 4. Assemble the prompt
        String systemPrompt = promptRegistry.getSystemPrompt(appId);
        String context = assembleContext(searchResults);
        String userMessage = assembleUserMessage(request.getMessage(), context, config.isIncludeSources());

        // 5. Call Gemini REST API
        String answer = callGemini(systemPrompt, userMessage, config);

        // 6. Cache the completion
        cachingService.cacheCompletion(request.getMessage(), appId, sessionId, answer);

        // 7. Build response
        return CompletionResponse.builder()
                .answer(answer)
                .sources(searchResults)
                .model(config.getModel())
                .tokensUsed(0)
                .appId(appId)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String assembleContext(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "No relevant context found in the indexed content.";
        }

        return results.stream()
                .map(r -> String.format(
                        "[Source: %s (relevance: %.0f%%)]\n%s",
                        r.getCitation(),
                        r.getScore() * 100,
                        r.getChunk().getContent()
                ))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String assembleUserMessage(String query, String context, boolean includeSources) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Context from indexed content:\n\n");
        sb.append(context);
        sb.append("\n\n---\n\n");
        sb.append("## User's question:\n");
        sb.append(query);

        if (includeSources) {
            sb.append("\n\n---\n");
            sb.append("Please cite your sources using the [Source: ...] references provided above.");
        }

        return sb.toString();
    }

    private String callGemini(String systemPrompt, String userMessage, PromptConfig.AppPromptConfig config) {
        ensureConfigured();

        try {
            String url = API_BASE + "/models/" + config.getModel() + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", userMessage))
                            )
                    ),
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "generationConfig", Map.of(
                            "temperature", config.getTemperature(),
                            "maxOutputTokens", config.getMaxCompletionTokens()
                    )
            );

            HttpEntity<String> httpRequest = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            ResponseEntity<String> response = restTemplate.postForEntity(url, httpRequest, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // Extract text from response
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String answer = parts.get(0).path("text").asText();
                    log.info("Gemini completion: {} chars response", answer.length());
                    return answer;
                }
            }

            throw new RuntimeException("Empty response from Gemini");

        } catch (Exception e) {
            log.error("Gemini completion failed: {}", e.getMessage());
            throw new RuntimeException("LLM completion failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY not configured");
        }
    }
}
