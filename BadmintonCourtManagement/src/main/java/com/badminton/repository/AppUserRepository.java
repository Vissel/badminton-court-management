package com.badminton.repository;

import com.badminton.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Accounts that have not been soft-deleted — the only ones user management
     * lists. The column is written once (never cleared), so the predicate is a
     * plain {@code IS NULL} instead of an OR.
     */
    @Query("SELECT u FROM AppUser u WHERE u.deleted IS NULL ORDER BY u.username")
    List<AppUser> findAllActive();

    @Query("SELECT u FROM AppUser u WHERE u.userId = :userId AND u.deleted IS NULL")
    Optional<AppUser> findActiveById(@Param("userId") Long userId);

    @Query("SELECT u FROM AppUser u WHERE u.username = :username AND u.deleted IS NULL")
    Optional<AppUser> findActiveByUsername(@Param("username") String username);
}
