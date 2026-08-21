package com.aiservice.platform.aiplatform.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to generate an AI completion.
 * The AI Platform uses the appId to load the correct system prompt and config.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletionRequest {

    /** The application requesting the completion (e.g., "insighttube", "pdfmind"). */
    @NotBlank(message = "appId is required")
    private String appId;

    /** The chat/session this completion belongs to. */
    private UUID sessionId;

    /** The user's question or message. */
    @NotBlank(message = "message is required")
    private String message;

    /** Optional: specific content session to search within (e.g., a particular video or PDF). */
    private UUID contentSessionId;

    /** Optional: override the default model for this request. */
    private String model;

    /** Optional: override the default temperature. */
    private Double temperature;

    /** Optional: override top-k retrieval. */
    private Integer topK;
}
