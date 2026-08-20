package com.aiservice.platform.identity.controller;

import com.aiservice.platform.identity.dto.request.ForgotPasswordRequest;
import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.RefreshTokenRequest;
import com.aiservice.platform.identity.dto.request.RegisterRequest;
import com.aiservice.platform.identity.dto.request.ResetPasswordRequest;
import com.aiservice.platform.identity.dto.request.VerifyEmailRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration, login, token management, password reset, and email verification")
public class AuthController {
    private final AuthService authService;



    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns access + refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest)
    {
        AuthResponse response = authService.registerUser(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "User registered successfully",
                        response
                ));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a user and returns access + refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        response
                )
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Exchanges a valid refresh token for new access + refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request)
    {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Token refresh successful",
                        response
                )
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the current session's refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> logout(@Valid @RequestBody RefreshTokenRequest request)
    {
        authService.logout(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logged out of this session",
                        null
                )
        );
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all sessions", description = "Revokes all refresh tokens and increments token version")
    public ResponseEntity<ApiResponse<AuthResponse>> logoutAll()
    {
        authService.logoutAll();
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logged out from everywhere",
                        null
                )
        );
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Sends a password reset email if the user exists. Always returns 200 to prevent email enumeration.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "If the email is registered, a password reset link has been sent",
                        null
                )
        );
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets the user's password using a valid reset token. Invalidates all sessions.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Password has been reset successfully",
                        null
                )
        );
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address", description = "Marks the user's email as verified using a verification token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        authService.verifyEmail(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Email verified successfully",
                        null
                )
        );
    }
}
