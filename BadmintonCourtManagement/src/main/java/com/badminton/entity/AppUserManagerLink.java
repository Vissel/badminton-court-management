package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Manager-subordinate mapping (1 user manages n users). An administrator is
 * linked to the coordinators they manage so that GET /api/v1/users can be
 * scoped per administrator; root bypasses the mapping and sees everyone.
 */
@Entity
@Table(name = "app_user_manager_link")
@Getter
@Setter
@NoArgsConstructor
public class AppUserManagerLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long linkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_user_id", nullable = false)
    private AppUser manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "managed_user_id", nullable = false)
    private AppUser managedUser;

    @Column(updatable = false, insertable = false)
    private Timestamp createdDate;
}
