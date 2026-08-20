package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.dto.request.AssignRolesRequest;
import com.aiservice.platform.identity.dto.request.RemoveRolesRequest;
import com.aiservice.platform.identity.dto.response.UserResponse;
import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.Role;
import com.aiservice.platform.identity.entity.User;
import com.aiservice.platform.identity.entity.UserClientRole;
import com.aiservice.platform.identity.enums.ErrorCode;
import com.aiservice.platform.identity.exception.BadRequestException;
import com.aiservice.platform.identity.exception.UserNotFoundException;
import com.aiservice.platform.identity.mapper.UserMapper;
import com.aiservice.platform.identity.repository.ClientRepository;
import com.aiservice.platform.identity.repository.RoleRepository;
import com.aiservice.platform.identity.repository.UserClientRoleRepository;
import com.aiservice.platform.identity.repository.UserRepository;
import com.aiservice.platform.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final UserClientRoleRepository userClientRoleRepository;

    @Override
    @Transactional
    public UserResponse assignRoles(UUID userId, String clientId, AssignRolesRequest request) {
        log.info("Assigning roles to user: userId={}, clientId={}, roles={}", userId, clientId, request.roles());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new UserNotFoundException("Client not found with clientId: " + clientId));

        for (String roleName : request.roles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException(ErrorCode.BAD_REQUEST, "Role not found: " + roleName));

            if (!userClientRoleRepository.existsByUserAndClientAndRole(user, client, role)) {
                UserClientRole assignment = UserClientRole.builder()
                        .user(user)
                        .client(client)
                        .role(role)
                        .build();
                userClientRoleRepository.save(assignment);
                log.info("Assigned role {} to user {} for client {}", roleName, userId, clientId);
            } else {
                log.debug("Role {} already assigned to user {} for client {}", roleName, userId, clientId);
            }
        }

        // Fetch fresh user with roles
        List<UserClientRole> userRoles = userClientRoleRepository.findAllByUserAndClient(user, client);
        Set<String> roleNames = userRoles.stream()
                .map(ucr -> ucr.getRole().getName())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getEmailVerified(),
                roleNames,
                user.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public UserResponse removeRoles(UUID userId, String clientId, RemoveRolesRequest request) {
        log.info("Removing roles from user: userId={}, clientId={}, roles={}", userId, clientId, request.roles());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new UserNotFoundException("Client not found with clientId: " + clientId));

        for (String roleName : request.roles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException(ErrorCode.BAD_REQUEST, "Role not found: " + roleName));

            if (userClientRoleRepository.existsByUserAndClientAndRole(user, client, role)) {
                userClientRoleRepository.deleteByUserAndClientAndRole(user, client, role);
                log.info("Removed role {} from user {} for client {}", roleName, userId, clientId);
            } else {
                log.debug("Role {} not assigned to user {} for client {}", roleName, userId, clientId);
            }
        }

        // Fetch fresh user with roles
        List<UserClientRole> userRoles = userClientRoleRepository.findAllByUserAndClient(user, client);
        Set<String> roleNames = userRoles.stream()
                .map(ucr -> ucr.getRole().getName())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                user.getEmailVerified(),
                roleNames,
                user.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserRoles(UUID userId, String clientId) {
        log.debug("Fetching roles for user: userId={}, clientId={}", userId, clientId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new UserNotFoundException("Client not found with clientId: " + clientId));

        return userClientRoleRepository.findAllByUserAndClient(user, client)
                .stream()
                .map(ucr -> ucr.getRole().getName())
                .collect(Collectors.toList());
    }
}
