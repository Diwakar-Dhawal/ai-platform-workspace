package com.aiservice.platform.identity.service;

import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.User;
import com.aiservice.platform.identity.enums.TokenType;
import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface JwtService {

    String generateAccessToken(
            User user,
            Client client,
            Set<String> roles,
            UUID sessionId
    );

    UUID extractUserId(String token);

    String extractUsername(String token);

    String extractClientId(String token);

    Set<String> extractRoles(String token);

    Instant extractIssuedAt(String token);

    Instant extractExpiration(String token);

    UUID extractTokenId(String token);

    TokenType extractTokenType(String token);

    UUID extractSessionId(String token);

    Integer extractTokenVersion(String token);

    Claims extractClaims(String token);

}