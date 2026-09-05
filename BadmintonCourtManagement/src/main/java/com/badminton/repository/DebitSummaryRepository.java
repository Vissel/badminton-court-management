package com.badminton.repository;

import com.badminton.entity.DebitSummary;
import com.badminton.entity.Player;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DebitSummaryRepository extends JpaRepository<DebitSummary, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<DebitSummary> findByPlayerAndIsActiveTrue(Player player);

}
