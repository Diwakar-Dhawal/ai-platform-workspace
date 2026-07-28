package com.aiservice.platform.identity.dto.response;

import com.aiservice.platform.identity.enums.HealthStatus;

import java.time.Instant;

public record HealthResponse(
        String service,
        HealthStatus status,
        String version,
        Instant timestamp
){}
