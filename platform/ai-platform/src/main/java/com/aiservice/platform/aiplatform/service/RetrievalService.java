package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.model.SearchResult;

import java.util.List;
import java.util.UUID;

/**
 * Retrieves relevant context for a user query using RAG.
 * Combines embedding generation + vector search into a single operation.
 */
public interface RetrievalService {

    /**
     * Retrieve relevant chunks for a query within an application.
     *
     * @param query the user's question
     * @param appId the application context
     * @param topK number of results
     * @return ranked search results with citations
     */
    List<SearchResult> retrieve(String query, String appId, int topK);

    /**
     * Retrieve relevant chunks within a specific content session.
     */
    List<SearchResult> retrieveWithinSession(
            String query,
            UUID contentSessionId,
            int topK
    );
}
