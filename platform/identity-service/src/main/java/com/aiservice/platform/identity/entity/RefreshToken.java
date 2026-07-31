package com.aiservice.platform.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name="idx_refresh_user", columnList="user_id"),
                @Index(name="idx_refresh_token", columnList="token")
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


    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;

    @Column
    private Instant revokedAt;

    @Column(length = 255)
    private String deviceName;

    @Column(length = 45)
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
