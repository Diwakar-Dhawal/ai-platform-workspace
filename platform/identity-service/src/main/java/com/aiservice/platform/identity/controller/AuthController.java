package com.aiservice.platform.identity.controller;

import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.RefreshTokenRequest;
import com.aiservice.platform.identity.dto.request.RegisterRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;



    @PostMapping("/register")
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
    public ResponseEntity<ApiResponse<AuthResponse>> logoutAll(@Valid @RequestParam String userId)
    {
        authService.logoutAll(userId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logged out from everywhere",
                        null
                )
        );
    }

}
