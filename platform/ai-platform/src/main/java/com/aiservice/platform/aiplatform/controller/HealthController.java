package com.aiservice.platform.aiplatform.controller;

import com.aiservice.platform.aiplatform.service.PromptRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final PromptRegistry promptRegistry;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", "ai-platform",
                "status", "UP",
                "version", "0.0.1",
                "registeredApps", promptRegistry.getRegisteredApps(),
                "timestamp", Instant.now()
        );
    }
}
