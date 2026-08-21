package com.aiservice.applications.insighttube.tubeservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "tube-service",
                "status", "UP",
                "version", "0.0.1",
                "timestamp", Instant.now()
        );
    }
}
