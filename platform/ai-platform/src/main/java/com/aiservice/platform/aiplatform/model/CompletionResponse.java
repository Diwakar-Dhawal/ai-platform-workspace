package com.aiservice.platform.aiplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI completion response with the answer and source citations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletionResponse {

    /** The AI-generated answer. */
    private String answer;

    /** Source chunks that informed the answer (with citations). */
    private List<SearchResult> sources;

    /** The model used for this completion. */
    private String model;

    /** Tokens used in the completion. */
    private Integer tokensUsed;

    /** The application this response belongs to. */
    private String appId;
}
