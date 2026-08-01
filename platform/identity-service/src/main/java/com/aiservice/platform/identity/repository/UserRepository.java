package com.aiservice.platform.identity.repository;

import com.aiservice.platform.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Modifying
    @Query("""
    update User u
    set u.tokenVersion = u.tokenVersion + 1
    where u.id = :userId
""")
    void incrementTokenVersion(@Param("userId") UUID userId);

}
