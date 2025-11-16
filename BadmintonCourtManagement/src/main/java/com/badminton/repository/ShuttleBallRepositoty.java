package com.badminton.repository;

import com.badminton.entity.ShuttleBall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShuttleBallRepositoty extends JpaRepository<ShuttleBall, Integer> {
    List<ShuttleBall> findAllByIsActive(boolean isActive);

    Optional<ShuttleBall> findByShuttleNameAndIsActive(String shuttleName, boolean isActive);

    List<ShuttleBall> findAllByShuttleName(String shuttleName);

    List<ShuttleBall> findAllByShuttleNameAndCostAndIsActiveTrue(String shuttleName, float cost);

    Optional<ShuttleBall> findByIsSelectedTrueAndIsActiveTrue();
}
