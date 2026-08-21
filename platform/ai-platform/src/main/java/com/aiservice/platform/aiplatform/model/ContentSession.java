package com.aiservice.platform.aiplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an ingested content session.
 * Each session is a piece of content that has been chunked and vectorized.
 *
 * Examples:
 *   InsightTube: A video, playlist, or channel
 *   PDFMind: A PDF document
 *   WebsiteMind: A website or set of pages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentSession {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    /** The application that owns this content. */
    private String appId;

    /** The user who ingested this content. */
    private UUID userId;

    /** Human-readable title (e.g., "React Tutorial - Full Course", "System Design Interview Guide"). */
    private String title;

    /** The source URL or identifier. */
    private String sourceUrl;

    /** Content type: video, playlist, channel, pdf, website, etc. */
    private String contentType;

    /** Number of chunks created during ingestion. */
    private int chunkCount;

    /** Total tokens in the content. */
    private long totalTokens;

    /** Application-specific metadata. */
    private Map<String, Object> metadata;

    /** Ingestion status: pending, processing, completed, failed. */
    @Builder.Default
    private String status = "pending";

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant completedAt;
}
