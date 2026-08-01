package com.aiservice.platform.identity.dto.response;

import java.time.Instant;

public record AuthResponse(

        String accessToken,

        String refreshToken,

        Instant accessTokenExpiresAt,

        Instant refreshTokenExpiresAt,

        UserResponse user

) {}