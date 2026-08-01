package com.aiservice.platform.identity.entity;

import com.aiservice.platform.identity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class User extends BaseEntity{
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Builder.Default
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<UserClientRole> userClientRoles = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private Integer tokenVersion = 0;

    @Builder.Default
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    public void addRole(
            Client client,
            Role role,
            UUID assignedBy
    ) {

        boolean alreadyAssigned = userClientRoles.stream()
                .anyMatch(userClientRole ->
                        userClientRole.getClient().equals(client)
                                && userClientRole.getRole().equals(role));

        if (alreadyAssigned) {
            return;
        }

        UserClientRole assignment = UserClientRole.builder()
                .user(this)
                .client(client)
                .role(role)
                .assignedBy(assignedBy)
                .build();

        this.userClientRoles.add(assignment);
        client.getUserClientRoles().add(assignment);
        role.getUserClientRoles().add(assignment);
    }

    public void removeRole(Client client, Role role) {

        userClientRoles.removeIf(userClientRole -> {

            boolean shouldRemove =
                    userClientRole.getClient().equals(client)
                            && userClientRole.getRole().equals(role);

            if (shouldRemove) {
                client.getUserClientRoles().remove(userClientRole);
                role.getUserClientRoles().remove(userClientRole);
            }

            return shouldRemove;
        });
    }

    public void markDeleted() {

        this.status = UserStatus.DELETED;

        this.refreshTokens.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        });
    }

    public void incrementTokenVersion() {
        tokenVersion++;
    }

}
