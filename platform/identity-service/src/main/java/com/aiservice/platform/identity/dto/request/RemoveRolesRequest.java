package com.aiservice.platform.identity.dto.request;

import com.aiservice.platform.identity.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RemoveRolesRequest(
        @NotEmpty
        List<RoleName> roles
) {}
