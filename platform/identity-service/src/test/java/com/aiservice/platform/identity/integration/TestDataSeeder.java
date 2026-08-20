package com.aiservice.platform.identity.integration;

import com.aiservice.platform.identity.entity.*;
import com.aiservice.platform.identity.enums.UserStatus;
import com.aiservice.platform.identity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class TestDataSeeder {

    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserClientRoleRepository userClientRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public Client seedClient(String clientId, String name) {
        if (clientRepository.existsByClientId(clientId)) {
            return clientRepository.findByClientId(clientId).orElseThrow();
        }
        Client client = Client.builder()
                .clientId(clientId)
                .name(name)
                .description(name + " Application")
                .build();
        return clientRepository.save(client);
    }

    public Role seedRole(String roleName, String description) {
        if (roleRepository.existsByName(roleName)) {
            return roleRepository.findByName(roleName).orElseThrow();
        }
        Role role = Role.builder()
                .name(roleName)
                .description(description)
                .build();
        return roleRepository.save(role);
    }

    public void seedAdminUser(String email, String password, String clientId) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        Client client = seedClient(clientId, "Admin Client");
        Role adminRole = seedRole("ADMIN", "Application administrator");
        Role userRole = seedRole("USER", "Default application user");

        User user = User.builder()
                .email(email)
                .username(email.split("@")[0])
                .passwordHash(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .tokenVersion(0)
                .build();
        User savedUser = userRepository.save(user);

        UserClientRole userClientRole = UserClientRole.builder()
                .user(savedUser)
                .client(client)
                .role(adminRole)
                .build();
        userClientRoleRepository.save(userClientRole);

        UserClientRole userRoleAssignment = UserClientRole.builder()
                .user(savedUser)
                .client(client)
                .role(userRole)
                .build();
        userClientRoleRepository.save(userRoleAssignment);
    }

    public void seedAll() {
        seedClient("test-client", "Test Client");
        seedRole("USER", "Default application user");
        seedRole("ADMIN", "Application administrator");
    }
}
