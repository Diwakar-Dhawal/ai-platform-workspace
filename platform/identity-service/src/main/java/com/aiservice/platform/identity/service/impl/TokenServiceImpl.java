package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.entity.*;
import com.aiservice.platform.identity.enums.ErrorCode;
import com.aiservice.platform.identity.exception.UnauthorizedException;
import com.aiservice.platform.identity.mapper.UserMapper;
import com.aiservice.platform.identity.repository.RefreshTokenRepository;
import com.aiservice.platform.identity.repository.UserClientRoleRepository;
import com.aiservice.platform.identity.repository.UserRepository;
import com.aiservice.platform.identity.security.TokenGenerator;
import com.aiservice.platform.identity.service.JwtService;
import com.aiservice.platform.identity.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenServiceImpl implements TokenService {
    private final JwtService jwtService;

    private final TokenGenerator tokenGenerator;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserClientRoleRepository userClientRoleRepository;

    private final UserRepository userRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public AuthResponse issueTokens(
            User user,
            Client client,
            UUID sessionId
    ) {

        List<UserClientRole> mappings =
                userClientRoleRepository.findAllByUserAndClient(
                        user,
                        client
                );

        if (mappings.isEmpty()) {
            log.error("No roles assigned for user: {} clientId: {}", user.getId(), client.getClientId());
            throw new IllegalStateException(
                    "User has no roles assigned for client: "
                            + client.getClientId()
            );
        }

        Set<String> roles = mappings.stream()
                .map(UserClientRole::getRole)
                .map(Role::getName)
                .collect(Collectors.toSet());

        String refreshTokenValue = tokenGenerator.generateRefreshToken();

        Instant now = Instant.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .expiresAt(
                        now.plusMillis(refreshTokenExpiration)
                )
                .user(user)
                .client(client)
                .sessionId(sessionId)
                .build();

        RefreshToken savedRefreshToken =
                refreshTokenRepository.save(refreshToken);

        log.debug("Tokens issued — userId: {} clientId: {} sessionId: {}", user.getId(), client.getClientId(), sessionId);

        String accessToken =
                jwtService.generateAccessToken(
                        user,
                        client,
                        roles,
                        savedRefreshToken.getSessionId()
                );

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                jwtService.extractExpiration(accessToken),
                savedRefreshToken.getExpiresAt(),
                UserMapper.toResponse(user)
        );
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        RefreshToken savedRefreshToken =
                refreshTokenRepository.findByToken(refreshToken)
                        .orElseThrow(() -> {
                            log.warn("Refresh failed — invalid refresh token");
                            return new UnauthorizedException(
                                    ErrorCode.INVALID_REFRESH_TOKEN,
                                    "Invalid refresh token"
                            );
                        });
        if (savedRefreshToken.getRevoked()) {
            log.warn("Refresh failed — token revoked for userId: {}", savedRefreshToken.getUser().getId());
            throw new UnauthorizedException(
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    "Refresh token has been revoked"
            );
        }
        if (savedRefreshToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh failed — token expired for userId: {}", savedRefreshToken.getUser().getId());
            savedRefreshToken.revoke();
            throw new UnauthorizedException(
                    ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Refresh token expired"
            );
        }

        User user = savedRefreshToken.getUser();
        Client client = savedRefreshToken.getClient();

        savedRefreshToken.revoke();

        log.info("Token refreshed — userId: {} clientId: {}", user.getId(), client.getClientId());
        return issueTokens(user, client,savedRefreshToken.getSessionId());
    }

    @Override
    public void logout(String refreshToken) {

        RefreshToken currentToken =
                refreshTokenRepository.findByToken(refreshToken)
                        .orElseThrow(() -> {
                            log.warn("Logout failed — invalid refresh token");
                            return new UnauthorizedException(
                                    ErrorCode.INVALID_REFRESH_TOKEN,
                                    "Invalid refresh token"
                            );
                        });

        refreshTokenRepository
                .findAllBySessionId(currentToken.getSessionId())
                .forEach(RefreshToken::revoke);

        log.info("User logged out — userId: {} sessionId: {}", currentToken.getUser().getId(), currentToken.getSessionId());
    }

    @Override
    public void logoutAll(UUID userId) {

        Instant now = Instant.now();

        refreshTokenRepository.findAllByUserId(userId)
                .stream()
                .filter(token -> !token.getRevoked())
                .forEach(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(now);
                });

        userRepository.incrementTokenVersion(userId);
        log.info("All sessions revoked — userId: {}", userId);
    }
}
