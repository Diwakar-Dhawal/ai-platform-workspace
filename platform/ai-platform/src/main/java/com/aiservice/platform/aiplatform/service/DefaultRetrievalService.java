package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.config.PromptConfig;
import com.aiservice.platform.aiplatform.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Default RAG retrieval: embed query → search Pinecone → return results.
 * Uses the per-app config for top-k and similarity threshold.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultRetrievalService implements RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final PromptRegistry promptRegistry;
    private final CachingService cachingService;

    @Override
    public List<SearchResult> retrieve(String query, String appId, int topK) {
        // 1. Check search cache
        // (Cache key includes appId since same query in different apps returns different results)

        // 2. Embed the query
        List<Float> queryEmbedding = embeddingService.embed(query);

        // 3. Get app config for similarity threshold
        PromptConfig.AppPromptConfig config = promptRegistry.getConfig(appId);
        double threshold = config.getSimilarityThreshold();

        // 4. Search vector store
        List<SearchResult> results = vectorStoreService.search(
                queryEmbedding, appId, topK, threshold
        );

        log.info("Retrieved {} results for query in app '{}' (topK={}, threshold={})",
                results.size(), appId, topK, threshold);

        return results;
    }

    @Override
    public List<SearchResult> retrieveWithinSession(
            String query,
            UUID contentSessionId,
            int topK
    ) {
        List<Float> queryEmbedding = embeddingService.embed(query);

        List<SearchResult> results = vectorStoreService.searchWithinSession(
                queryEmbedding, contentSessionId, topK, 0.0
        );

        log.info("Retrieved {} results for query within session {}", results.size(), contentSessionId);

        return results;
    }
}
