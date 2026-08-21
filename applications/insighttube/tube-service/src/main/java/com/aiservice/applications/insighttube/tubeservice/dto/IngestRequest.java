package com.aiservice.applications.insighttube.tubeservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestRequest {

    @NotBlank(message = "YouTube URL is required")
    private String url;

    @Builder.Default
    private List<String> languages = List.of("en");
}
