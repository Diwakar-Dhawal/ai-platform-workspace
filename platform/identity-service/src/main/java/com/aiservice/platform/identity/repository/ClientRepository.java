package com.aiservice.platform.identity.repository;

import com.aiservice.platform.identity.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByClientId(String ClientId);

    boolean existsByClientId(String ClientId);

}