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
import com.aiservice.platform.identity.enums.RoleName;
import com.aiservice.platform.identity.enums.UserStatus;
import com.aiservice.platform.identity.exception.BadRequestException;
import com.aiservice.platform.identity.exception.DuplicateResourceException;
import com.aiservice.platform.identity.exception.UnauthorizedException;
import com.aiservice.platform.identity.repository.*;
import com.aiservice.platform.identity.service.AuthService;
import com.aiservice.platform.identity.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;
    private static final RoleName DEFAULT_ROLE = RoleName.USER;
    private final TokenService tokenService;
    private final UserClientRoleRepository userClientRoleRepository;


    @Override
    public AuthResponse registerUser(RegisterRequest request) {
        if(userRepository.existsByEmail(request.email()))
        {
            throw new DuplicateResourceException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email Already Exists");
        }
        if(userRepository.existsByUsername(request.username()))
        {
            throw new DuplicateResourceException(ErrorCode.USERNAME_ALREADY_EXISTS, "Username Already Exists");
        }
        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Default USER role is missing. Check database seeder."
                        ));

        Client client = clientRepository.findByClientId(request.clientId())
                .orElseThrow(() ->
                        new BadRequestException(
                                ErrorCode.CLIENT_NOT_FOUND,
                                "Client not found"
                        ));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        user.addRole(client, role, null);
        User savedUser = userRepository.save(user);
        return tokenService.issueTokens(savedUser,client,  UUID.randomUUID());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                     new UnauthorizedException(
                            ErrorCode.INVALID_CREDENTIALS,
                            "Invalid email or password")

        );
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid email or password"
            );
        }
        if(user.getStatus() != UserStatus.ACTIVE)
        {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_INACTIVE, "User account is not active");
        }
        Client client = clientRepository.findByClientId(request.clientId())
                .orElseThrow(() ->
                        new BadRequestException(
                                ErrorCode.CLIENT_NOT_FOUND,
                                "Client not found"
                        ));
        List<UserClientRole> roles = userClientRoleRepository.findAllByUserAndClient(user,client);
        if(roles.isEmpty())
            throw new UnauthorizedException(ErrorCode.FORBIDDEN, "You are not authorized to access this application");
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
    public void logoutAll(String userId) {

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() ->
                        new UnauthorizedException(
                                ErrorCode.USER_NOT_FOUND,
                                "User not found"
                        )
                );

        tokenService.logoutAll(user.getId());
    }


}
