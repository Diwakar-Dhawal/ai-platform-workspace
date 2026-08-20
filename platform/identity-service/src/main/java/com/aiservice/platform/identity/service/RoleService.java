package com.aiservice.platform.identity.service;

import com.aiservice.platform.identity.dto.request.AssignRolesRequest;
import com.aiservice.platform.identity.dto.request.RemoveRolesRequest;
import com.aiservice.platform.identity.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    UserResponse assignRoles(UUID userId, String clientId, AssignRolesRequest request);

    UserResponse removeRoles(UUID userId, String clientId, RemoveRolesRequest request);

    List<String> getUserRoles(UUID userId, String clientId);
}
