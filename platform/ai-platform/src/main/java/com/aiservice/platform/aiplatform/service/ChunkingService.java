package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.model.Chunk;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chunks raw content into embeddable pieces.
 * Uses the app-specific chunk strategy from PromptConfig.
 *
 * Different applications need different chunking:
 *   InsightTube: chunk by timestamp/transcript segments
 *   PDFMind: chunk by page boundaries + semantic sections
 *   WebsiteMind: chunk by HTML sections/paragraphs
 */
public interface ChunkingService {

    /**
     * Chunk raw content into embeddable pieces.
     *
     * @param content the raw text content
     * @param appId determines the chunking strategy
     * @param sessionId the content session this belongs to
     * @param metadata base metadata to attach to each chunk
     * @return list of chunks ready for embedding
     */
    List<Chunk> chunk(
            String content,
            String appId,
            UUID sessionId,
            Map<String, Object> metadata
    );
}
