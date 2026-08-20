package com.aiservice.platform.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(

        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Client ID must contain only letters, numbers, dots, hyphens, or underscores")
        String clientId,

        @NotBlank
        @Size(min = 1, max = 100)
        String name,

        @Size(max = 500)
        String description

) {}
