package com.aiservice.applications.insighttube.tubeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestResponse {

    private String status;
    private UUID contentSessionId;
    private String title;
    private String sourceUrl;
    private int chunkCount;
    private String message;
}
