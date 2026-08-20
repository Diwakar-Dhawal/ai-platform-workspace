package com.aiservice.platform.identity.controller;

import com.aiservice.platform.identity.dto.response.HealthResponse;
import com.aiservice.platform.identity.service.HealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthController {
    private final HealthService healthService;
    public HealthController(HealthService healthService)
    {
        this.healthService = healthService;
    }
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth()
    {
        log.debug("Health check requested");
        return ResponseEntity.ok().body(healthService.getHealth());
    }
}
