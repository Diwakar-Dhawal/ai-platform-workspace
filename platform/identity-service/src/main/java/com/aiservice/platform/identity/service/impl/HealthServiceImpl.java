package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.dto.response.HealthResponse;
import com.aiservice.platform.identity.enums.HealthStatus;
import com.aiservice.platform.identity.service.HealthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class HealthServiceImpl implements HealthService {
    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${application.version}")
    private String applicationVersion;
    @Override
    public HealthResponse getHealth() {
        return new HealthResponse(applicationName, HealthStatus.UP, applicationVersion,  Instant.now());
    }
}
