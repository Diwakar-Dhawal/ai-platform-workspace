package com.aiservice.platform.identity.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateClientRequest(

        @Size(min = 1, max = 100)
        String name,

        @Size(max = 500)
        String description

) {}
