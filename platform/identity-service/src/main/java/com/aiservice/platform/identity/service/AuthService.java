package com.aiservice.platform.identity.service;

import com.aiservice.platform.identity.dto.request.ForgotPasswordRequest;
import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.RefreshTokenRequest;
import com.aiservice.platform.identity.dto.request.RegisterRequest;
import com.aiservice.platform.identity.dto.request.ResetPasswordRequest;
import com.aiservice.platform.identity.dto.request.VerifyEmailRequest;
import com.aiservice.platform.identity.dto.response.AuthResponse;

import java.util.UUID;

public interface AuthService {

    AuthResponse registerUser(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void logoutAll();

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(VerifyEmailRequest request);
}
