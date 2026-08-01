package com.aiservice.platform.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Role extends BaseEntity{


    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Builder.Default
    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY
    )
    private Set<UserClientRole> userClientRoles = new HashSet<>();
}
