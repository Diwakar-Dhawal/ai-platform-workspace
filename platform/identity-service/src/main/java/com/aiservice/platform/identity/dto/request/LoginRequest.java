package com.aiservice.platform.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password,

        @NotNull
        String clientId

) {}