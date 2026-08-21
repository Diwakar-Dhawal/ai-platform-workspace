package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.model.CompletionRequest;
import com.aiservice.platform.aiplatform.model.CompletionResponse;

/**
 * Generates AI completions using the per-application system prompt + retrieved context.
 * This is the main entry point for chat interactions.
 */
public interface CompletionService {

    /**
     * Generate a completion for a user query.
     * Loads the app-specific system prompt, retrieves context via RAG,
     * and calls the LLM with the combined context.
     *
     * @param request the completion request
     * @return the AI response with sources
     */
    CompletionResponse complete(CompletionRequest request);
}
