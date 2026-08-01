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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenServiceImpl implements TokenService {
    private final JwtService jwtService;

    private final TokenGenerator tokenGenerator;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserClientRoleRepository userClientRoleRepository;

    private final UserMapper userMapper;

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
                userMapper.toResponse(user)
        );
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        RefreshToken savedRefreshToken =
                refreshTokenRepository.findByToken(refreshToken)
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        ErrorCode.INVALID_REFRESH_TOKEN,
                                        "Invalid refresh token"
                                ));
        if (savedRefreshToken.getRevoked()) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    "Refresh token has been revoked"
            );
        }
        if (savedRefreshToken.getExpiresAt().isBefore(Instant.now())) {

            savedRefreshToken.revoke();

            throw new UnauthorizedException(
                    ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Refresh token expired"
            );
        }

        User user = savedRefreshToken.getUser();
        Client client = savedRefreshToken.getClient();

        savedRefreshToken.revoke();


        return issueTokens(user, client,savedRefreshToken.getSessionId());
    }

    @Override
    public void logout(String refreshToken) {

        RefreshToken currentToken =
                refreshTokenRepository.findByToken(refreshToken)
                        .orElseThrow(() ->
                                new UnauthorizedException(
                                        ErrorCode.INVALID_REFRESH_TOKEN,
                                        "Invalid refresh token"
                                ));

        refreshTokenRepository
                .findAllBySessionId(currentToken.getSessionId())
                .forEach(RefreshToken::revoke);
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
    }
}
