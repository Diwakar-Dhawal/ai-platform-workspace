package com.aiservice.platform.aiplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A chunk retrieved from vector search with its relevance score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /** The matched chunk. */
    private Chunk chunk;

    /** Cosine similarity score (0.0 - 1.0). */
    private double score;

    /** How this result should be cited (e.g., "Page 847", "Video at 12:34"). */
    private String citation;
}
