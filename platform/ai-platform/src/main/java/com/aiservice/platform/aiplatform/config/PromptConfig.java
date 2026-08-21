package com.aiservice.platform.aiplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads per-application prompt configurations from application.yaml.
 *
 * Each application registers its own prompt bundle under:
 *   ai-platform.prompts.{appId}
 *
 * Example:
 *   ai-platform:
 *     prompts:
 *       insighttube:
 *         system-prompt: "You are a YouTube content analyst..."
 *         chunk-strategy: transcript-by-timestamp
 *         top-k: 10
 *         similarity-threshold: 0.7
 *       pdfmind:
 *         system-prompt: "You are a PDF document analyst..."
 *         chunk-strategy: semantic-with-page-boundary
 *         top-k: 15
 *         similarity-threshold: 0.75
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai-platform")
public class PromptConfig {

    /**
     * Map of appId → AppPromptConfig.
     * Each entry defines how the AI Platform behaves for a specific application.
     */
    private Map<String, AppPromptConfig> prompts = new HashMap<>();

    @Data
    public static class AppPromptConfig {

        /**
         * The system prompt that defines the AI's role for this application.
         */
        private String systemPrompt;

        /**
         * How content is chunked during ingestion.
         * Examples: transcript-by-timestamp, semantic-with-page-boundary, fixed-size, paragraph-based
         */
        private String chunkStrategy;

        /**
         * Metadata fields to extract and store with each chunk.
         * These are application-specific (e.g., page_number for PDFMind, timestamp for InsightTube).
         */
        private java.util.List<String> metadataFields = java.util.List.of();

        /**
         * Default chunk size in tokens for this application.
         */
        private int chunkSize = 512;

        /**
         * Overlap between consecutive chunks (in tokens).
         */
        private int chunkOverlap = 100;

        /**
         * Number of top results to retrieve during RAG.
         */
        private int topK = 10;

        /**
         * Minimum similarity score threshold (0.0 - 1.0) for retrieval.
         */
        private double similarityThreshold = 0.7;

        /**
         * The LLM model to use for this application's completions.
         */
        private String model = "gpt-4o-mini";

        /**
         * Maximum tokens for the completion response.
         */
        private int maxCompletionTokens = 2048;

        /**
         * Temperature for the LLM (0.0 = deterministic, 1.0 = creative).
         */
        private double temperature = 0.3;

        /**
         * Whether to include source citations in responses.
         */
        private boolean includeSources = true;

        /**
         * Format for source citations.
         * Examples: page-number, timestamp, section-title, custom
         */
        private String citationFormat = "page-number";
    }

    /**
     * Get prompt config for an application. Throws if app not registered.
     */
    public AppPromptConfig getConfigForApp(String appId) {
        AppPromptConfig config = prompts.get(appId);
        if (config == null) {
            throw new IllegalArgumentException(
                    "No AI prompt configuration registered for app: " + appId
                            + ". Registered apps: " + prompts.keySet()
            );
        }
        return config;
    }

    /**
     * Check if an application has a registered prompt configuration.
     */
    public boolean isAppRegistered(String appId) {
        return prompts.containsKey(appId);
    }

    /**
     * Get all registered application IDs.
     */
    public java.util.Set<String> getRegisteredApps() {
        return prompts.keySet();
    }
}
