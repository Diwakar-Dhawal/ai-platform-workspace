package com.aiservice.platform.identity.repository;

import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.Role;
import com.aiservice.platform.identity.entity.User;
import com.aiservice.platform.identity.entity.UserClientRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserClientRoleRepository extends JpaRepository<UserClientRole, UUID> {
    List<UserClientRole> findAllByUserAndClient(User user, Client client);

    boolean existsByUserAndClientAndRole(User user, Client client, Role role);

    void deleteByUserAndClientAndRole(User user, Client client, Role role);
}
