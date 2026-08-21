package com.aiservice.applications.insighttube.tubeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String answer;
    private List<SourceRef> sources;
    private String model;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceRef {
        private String citation;
        private double relevance;
        private String content;
    }
}
