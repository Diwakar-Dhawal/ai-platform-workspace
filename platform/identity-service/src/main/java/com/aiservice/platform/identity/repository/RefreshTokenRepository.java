package com.aiservice.platform.identity.repository;

import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.entity.RefreshToken;
import com.aiservice.platform.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUser(User user);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    List<RefreshToken> findAllByUserId(UUID userId);

    List<RefreshToken> findAllByUserAndClient(User user, Client client);

    List<RefreshToken> findAllBySessionId(UUID sessionId);
}