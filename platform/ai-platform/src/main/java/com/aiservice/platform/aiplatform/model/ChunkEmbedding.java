package com.aiservice.platform.aiplatform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Stores chunk content + embedding vector in PostgreSQL.
 * Used as a fallback when Pinecone is not configured.
 * Cosine similarity search is done in Java for MVP scale (<100K vectors).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chunk_embeddings", indexes = {
        @Index(name = "idx_chunk_embeddings_session", columnList = "sessionId"),
        @Index(name = "idx_chunk_embeddings_app", columnList = "appId"),
        @Index(name = "idx_chunk_embeddings_app_session", columnList = "appId, sessionId")
})
public class ChunkEmbedding {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private String appId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(nullable = false)
    private int totalChunks;

    /**
     * Embedding vector stored as float array.
     * PostgreSQL supports real[] which maps well to float arrays.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", columnDefinition = "real[]")
    private Float[] embedding;

    /**
     * Additional metadata as JSON string (timestamp, videoTitle, etc.)
     */
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
}
