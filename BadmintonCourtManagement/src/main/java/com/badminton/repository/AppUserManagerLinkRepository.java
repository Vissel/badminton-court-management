package com.badminton.repository;

import com.badminton.entity.AppUserManagerLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserManagerLinkRepository extends JpaRepository<AppUserManagerLink, Long> {
    List<AppUserManagerLink> findByManager_UserId(Long managerUserId);

    Optional<AppUserManagerLink> findByManagedUser_UserId(Long managedUserId);

    void deleteByManagedUser_UserId(Long managedUserId);

    /** Links where the given user is the manager (used when that account is removed). */
    void deleteByManager_UserId(Long managerUserId);
}
