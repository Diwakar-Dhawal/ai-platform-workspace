package com.aiservice.platform.identity.integration;

import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.Role;
import com.aiservice.platform.identity.repository.ClientRepository;
import com.aiservice.platform.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class TestDataSeeder {

    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;

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

    public void seedAll() {
        seedClient("test-client", "Test Client");
        seedRole("USER", "Default application user");
        seedRole("ADMIN", "Application administrator");
    }
}
