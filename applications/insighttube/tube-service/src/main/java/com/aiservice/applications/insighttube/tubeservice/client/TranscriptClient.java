package com.aiservice.applications.insighttube.tubeservice.client;

import com.aiservice.applications.insighttube.tubeservice.dto.TranscriptResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls the Python Transcript Service to extract YouTube transcripts.
 */
@Slf4j
@Component
public class TranscriptClient {

    @Value("${services.transcript-service.url}")
    private String transcriptServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranscriptResponse extractTranscript(String url, java.util.List<String> languages) {
        try {
            Map<String, Object> body = Map.of("url", url, "languages", languages);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    createHeaders()
            );

            log.info("Calling transcript service for URL: {}", url);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    transcriptServiceUrl + "/extract",
                    request,
                    String.class
            );

            TranscriptResponse result = objectMapper.readValue(response.getBody(), TranscriptResponse.class);
            log.info("Transcript extracted: {} videos, {} segments, cached={}",
                    result.getTotalVideos(), result.getTotalSegments(), result.isCached());

            return result;

        } catch (Exception e) {
            log.error("Failed to extract transcript from {}: {}", url, e.getMessage());
            throw new RuntimeException("Transcript extraction failed: " + e.getMessage(), e);
        }
    }

    public boolean isHealthy() {
        try {
            restTemplate.getForObject(transcriptServiceUrl + "/health", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
