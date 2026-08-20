package com.aiservice.platform.identity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String clientId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
