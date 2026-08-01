package com.aiservice.platform.identity.bootstrap;

import com.aiservice.platform.identity.entity.Role;
import com.aiservice.platform.identity.enums.RoleName;
import com.aiservice.platform.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {

        createRole(RoleName.USER, "Default application user");

        createRole(RoleName.MANAGER, "Application manager");

        createRole(RoleName.ADMIN, "Application administrator");
    }

    private void createRole(RoleName roleName, String description) {

        if (roleRepository.existsByName(roleName)) {
            return;
        }

        Role role = Role.builder()
                .name(roleName)
                .description(description)
                .build();

        roleRepository.save(role);
    }
}