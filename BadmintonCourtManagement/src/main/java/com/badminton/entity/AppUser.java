package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String displayName;

    /** External application identifier, reserved for future use. */
    @Column(name = "app_id", length = 50)
    private String appId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Soft-delete marker: set once the account is removed from user management.
     * NULL means active — every user query filters on {@code is_deleted IS NULL},
     * so a removed account keeps its row (and history) without being listed.
     */
    @Column(name = "is_deleted")
    private Boolean deleted;

    @Column(updatable = false, insertable = false)
    private Timestamp createdDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public AppUser(String username, String password, Role role) {
        this.username = username;
        this.displayName = username;
        this.password = password;
        this.roles.add(role);
    }

    /** True when this account has been removed from user management. */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
}
