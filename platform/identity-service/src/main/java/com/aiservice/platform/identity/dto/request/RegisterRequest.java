package com.aiservice.platform.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username must contain only letters, numbers, dots, hyphens, or underscores")
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
                message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character (@#$%^&+=)!"
        )
        String password,

        @NotBlank
        String clientId

) {}