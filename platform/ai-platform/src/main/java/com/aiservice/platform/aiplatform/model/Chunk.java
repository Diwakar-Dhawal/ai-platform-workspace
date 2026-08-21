package com.aiservice.platform.aiplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A chunk of content ready for embedding and storage.
 * Each chunk belongs to a content session and carries application-specific metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    /** The content session this chunk belongs to (e.g., a video, a PDF, a playlist). */
    private UUID sessionId;

    /** The application that owns this content (e.g., "insighttube", "pdfmind"). */
    private String appId;

    /** The actual text content of this chunk. */
    private String content;

    /** Index of this chunk within its source (e.g., chunk 5 of 20). */
    private int chunkIndex;

    /** Total number of chunks in the source. */
    private int totalChunks;

    /**
     * Application-specific metadata.
     * Examples:
     *   InsightTube: {video_id, timestamp, channel_name, video_title}
     *   PDFMind: {page_number, chapter, section_title, document_title}
     */
    private Map<String, Object> metadata;

    /** When this chunk was created. */
    @Builder.Default
    private Instant createdAt = Instant.now();
}
