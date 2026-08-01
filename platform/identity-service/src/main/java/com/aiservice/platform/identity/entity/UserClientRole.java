package com.aiservice.platform.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(
        name="user_client_roles",
        uniqueConstraints={
                @UniqueConstraint(
                        columnNames = {
                                "user_id",
                                "client_id",
                                "role_id"
                        }
                )
        },
        indexes = {

                @Index(
                        name = "idx_user_client_role_user",
                        columnList = "user_id"
                ),

                @Index(
                        name = "idx_user_client_role_role",
                        columnList = "role_id"
                ),

                @Index(
                        name = "idx_user_client_role_client",
                        columnList = "client_id"
                ),
                @Index(
                        name = "idx_user_client",
                        columnList = "user_id,client_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserClientRole extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = true)
    private UUID assignedBy;

    @CreationTimestamp
    private Instant assignedAt;
}
