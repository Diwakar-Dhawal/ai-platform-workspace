package com.aiservice.platform.identity.dto.response;

import com.aiservice.platform.identity.enums.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(

        UUID id,

        String username,

        String email,

        UserStatus status,

        Boolean emailVerified,

        Set<String> roles,

        Instant createdAt

) {}