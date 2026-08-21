package com.aiservice.platform.aiplatform.service;

import java.util.List;

/**
 * Converts text into vector embeddings.
 * Application-agnostic — the same embedding model is used for all applications.
 * Application-specific behavior is handled by chunk strategy, not embedding.
 */
public interface EmbeddingService {

    /**
     * Generate an embedding vector for a single text.
     *
     * @param text the text to embed
     * @return embedding vector as a list of floats
     */
    List<Float> embed(String text);

    /**
     * Generate embedding vectors for multiple texts (batch).
     *
     * @param texts list of texts to embed
     * @return list of embedding vectors, one per input text
     */
    List<List<Float>> embedBatch(List<String> texts);

    /**
     * Get the dimension of the embedding model.
     */
    int getDimensions();
}
