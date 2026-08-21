package com.aiservice.platform.identity.mapper;

import com.aiservice.platform.identity.dto.response.UserResponse;
import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.User;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {
    private UserMapper() {
    }

    /**
     * Map a User to a UserResponse with roles scoped to a single client.
     * Only roles assigned for the given client are included.
     */
    public static UserResponse toResponse(User user, Client client) {

        Set<String> roles =
                user.getUserClientRoles()
                        .stream()
                        .filter(userRole -> userRole.getClient().equals(client))
                        .map(userRole -> userRole.getRole().getName())
                        .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getEmailVerified(),
                roles,
                user.getCreatedAt()
        );
    }
}