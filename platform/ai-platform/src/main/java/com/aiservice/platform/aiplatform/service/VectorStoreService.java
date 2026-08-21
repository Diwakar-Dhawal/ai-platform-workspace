package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.model.Chunk;
import com.aiservice.platform.aiplatform.model.SearchResult;

import java.util.List;
import java.util.UUID;

/**
 * Stores and retrieves vector embeddings.
 * Application-agnostic — stores chunks with their metadata and supports similarity search.
 */
public interface VectorStoreService {

    /**
     * Store a chunk with its embedding vector.
     */
    void store(Chunk chunk, List<Float> embedding);

    /**
     * Store multiple chunks in batch.
     */
    void storeBatch(List<ChunkWithEmbedding> chunks);

    /**
     * Search for similar chunks using a query embedding.
     *
     * @param queryEmbedding the query vector
     * @param appId filter results to this application
     * @param topK number of results to return
     * @param similarityThreshold minimum similarity score
     * @return ranked list of search results
     */
    List<SearchResult> search(
            List<Float> queryEmbedding,
            String appId,
            int topK,
            double similarityThreshold
    );

    /**
     * Search within a specific content session.
     */
    List<SearchResult> searchWithinSession(
            List<Float> queryEmbedding,
            UUID contentSessionId,
            int topK,
            double similarityThreshold
    );

    /**
     * Delete all chunks for a content session.
     */
    void deleteSession(UUID contentSessionId);

    /**
     * Delete all chunks for an application.
     */
    void deleteApp(String appId);

    /**
     * Record pairing of chunk + embedding for batch storage.
     */
    record ChunkWithEmbedding(Chunk chunk, List<Float> embedding) {}
}
