package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.User;
import com.aiservice.platform.identity.enums.RoleName;
import com.aiservice.platform.identity.enums.TokenType;
import com.aiservice.platform.identity.service.JwtService;
import com.aiservice.platform.identity.util.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;


    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public String generateAccessToken(
            User user,
            Client client,
            Set<String> roles,
            UUID sessionId
    ) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .audience()
                .add(client.getClientId())
                .and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiration)))
                .id(UUID.randomUUID().toString())
                .claim(JwtClaims.USER_ID, user.getId())
                .claim(JwtClaims.USERNAME, user.getUsername())
                .claim(JwtClaims.TOKEN_VERSION, user.getTokenVersion())
                .claim(JwtClaims.SESSION_ID, sessionId.toString())
                .claim(JwtClaims.ROLES, roles)
                .claim(JwtClaims.TOKEN_TYPE, TokenType.ACCESS.name())
                .signWith(signingKey)
                .compact();
    }



    @Override
    public Claims extractClaims(String token) {

        return Jwts.parser()
                .clockSkewSeconds(30)
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(
                extractClaims(token)
                        .get(JwtClaims.USER_ID, String.class)
        );
    }

    @Override
    public String extractUsername(String token) {
        return extractClaims(token)
                .get(JwtClaims.USERNAME, String.class);
    }

    @Override
    public String extractClientId(String token) {
        return extractClaims(token)
                .getAudience()
                .iterator()
                .next();
    }

    @Override
    public Set<RoleName> extractRoles(String token) {

        List<String> roles = extractClaims(token)
                .get(JwtClaims.ROLES, List.class);

        return roles.stream()
                .map(RoleName::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Instant extractIssuedAt(String token) {
        return extractClaims(token)
                .getIssuedAt()
                .toInstant();
    }

    @Override
    public Instant extractExpiration(String token) {
        return extractClaims(token)
                .getExpiration()
                .toInstant();
    }

    @Override
    public UUID extractTokenId(String token) {
        return UUID.fromString(
                extractClaims(token).getId()
        );
    }

    @Override
    public TokenType extractTokenType(String token) {
        return TokenType.valueOf(
                extractClaims(token)
                        .get(JwtClaims.TOKEN_TYPE, String.class)
        );
    }

    @Override
    public UUID extractSessionId(String token) {

        return UUID.fromString(
                extractClaims(token)
                        .get(JwtClaims.SESSION_ID, String.class)
        );
    }

    @Override
    public Integer extractTokenVersion(String token) {

        return extractClaims(token)
                .get(JwtClaims.TOKEN_VERSION, Integer.class);
    }
}
