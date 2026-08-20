package com.aiservice.platform.identity.repository;

import com.aiservice.platform.identity.entity.User;
import com.aiservice.platform.identity.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByTokenAndTokenType(String token, VerificationToken.TokenType tokenType);

    void deleteByUserAndTokenType(User user, VerificationToken.TokenType tokenType);
}
