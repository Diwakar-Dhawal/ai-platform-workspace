package com.aiservice.platform.identity.mapper;

import com.aiservice.platform.identity.dto.response.UserResponse;
import com.aiservice.platform.identity.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {

        Set<String> roles =
                user.getUserClientRoles()
                        .stream()
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