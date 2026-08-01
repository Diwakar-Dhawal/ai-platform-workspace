package com.aiservice.platform.identity.service;

import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.User;

import java.util.UUID;

public interface TokenService {

    AuthResponse issueTokens(
            User user,
            Client client,
            UUID sessionId
    );

    AuthResponse refresh(
            String refreshToken
    );

    void logout(
            String refreshToken
    );

    void logoutAll(UUID userId);
}
