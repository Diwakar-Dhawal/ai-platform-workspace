package com.aiservice.platform.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_user", columnList = "user_id"),
                @Index(name = "idx_refresh_token", columnList = "token"),
                @Index(name = "idx_refresh_session", columnList = "session_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class RefreshToken extends BaseEntity{
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Builder.Default
    @Column(nullable = false)
    private UUID sessionId = UUID.randomUUID();

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column
    private Instant revokedAt;

    @Column(length = 255)
    private String deviceName;

    @Column(length = 45)
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void revoke() {
        revoked = true;
        revokedAt = Instant.now();
    }
}
