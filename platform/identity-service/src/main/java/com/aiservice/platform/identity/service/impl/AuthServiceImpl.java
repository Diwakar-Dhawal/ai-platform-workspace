package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.RefreshTokenRequest;
import com.aiservice.platform.identity.dto.request.RegisterRequest;
import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.Role;
import com.aiservice.platform.identity.entity.User;
import com.aiservice.platform.identity.entity.UserClientRole;
import com.aiservice.platform.identity.enums.ErrorCode;
import com.aiservice.platform.identity.enums.UserStatus;
import com.aiservice.platform.identity.exception.BadRequestException;
import com.aiservice.platform.identity.exception.DuplicateResourceException;
import com.aiservice.platform.identity.exception.UnauthorizedException;
import com.aiservice.platform.identity.repository.*;
import com.aiservice.platform.identity.security.CustomUserDetails;
import com.aiservice.platform.identity.service.AuthService;
import com.aiservice.platform.identity.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private static final String DEFAULT_ROLE = "USER";
    private final TokenService tokenService;
    private final UserClientRoleRepository userClientRoleRepository;


    @Override
    public AuthResponse registerUser(RegisterRequest request) {
        log.info("Registration attempt for email: {} username: {} clientId: {}", request.email(), request.username(), request.clientId());

        if(userRepository.existsByEmail(request.email()))
        {
            log.warn("Registration failed — email already exists: {}", request.email());
            throw new DuplicateResourceException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email Already Exists");
        }
        if(userRepository.existsByUsername(request.username()))
        {
            log.warn("Registration failed — username already exists: {}", request.username());
            throw new DuplicateResourceException(ErrorCode.USERNAME_ALREADY_EXISTS, "Username Already Exists");
        }
        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Default USER role is missing. Check database seeder."
                        ));

        Client client = clientRepository.findByClientId(request.clientId())
                .orElseThrow(() -> {
                    log.warn("Registration failed — client not found: {}", request.clientId());
                    return new BadRequestException(
                            ErrorCode.CLIENT_NOT_FOUND,
                            "Client not found"
                    );
                });

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        user.addRole(client, role, null);
        User savedUser = userRepository.save(user);
        log.info("User registered successfully — id: {} username: {} clientId: {}", savedUser.getId(), savedUser.getUsername(), client.getClientId());
        return tokenService.issueTokens(savedUser,client,  UUID.randomUUID());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {} clientId: {}", request.email(), request.clientId());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found for email: {}", request.email());
                    return new UnauthorizedException(
                            ErrorCode.INVALID_CREDENTIALS,
                            "Invalid email or password");
                });
        if(user.getStatus() != UserStatus.ACTIVE)
        {
            log.warn("Login failed — account inactive: {} status: {}", request.email(), user.getStatus());
            throw new UnauthorizedException(ErrorCode.ACCOUNT_INACTIVE, "User account is not active");
        }
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            log.warn("Login failed — invalid password for email: {}", request.email());
            throw new UnauthorizedException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid email or password"
            );
        }
        Client client = clientRepository.findByClientId(request.clientId())
                .orElseThrow(() -> {
                    log.warn("Login failed — client not found: {}", request.clientId());
                    return new BadRequestException(
                            ErrorCode.CLIENT_NOT_FOUND,
                            "Client not found"
                    );
                });
        List<UserClientRole> roles = userClientRoleRepository.findAllByUserAndClient(user,client);
        if(roles.isEmpty()) {
            log.warn("Login failed — no roles for user: {} clientId: {}", user.getId(), client.getClientId());
            throw new UnauthorizedException(ErrorCode.FORBIDDEN, "You are not authorized to access this application");
        }
        log.info("Login successful — userId: {} clientId: {}", user.getId(), client.getClientId());
        return tokenService.issueTokens(user, client, UUID.randomUUID());
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        return tokenService.refresh(request.refreshToken());
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        tokenService.logout(request.refreshToken());
    }

    @Override
    public void logoutAll() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException(
                    ErrorCode.UNAUTHORIZED,
                    "Authentication is required to log out from all sessions"
            );
        }

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();
        tokenService.logoutAll(user.getUserId());
    }


}
