package com.badminton.repository;

import com.badminton.entity.Debit;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebitRepository extends JpaRepository<Debit, Integer> {

    List<Debit> findByPlayer_PlayerId(Integer playerId);

    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId ORDER BY d.createdDate DESC")
    List<Debit> findByPlayerIdOrderByCreatedDateDesc(@Param("playerId") Integer playerId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId " +
           "AND (:from IS NULL OR d.createdDate >= :from) " +
           "AND (:to IS NULL OR d.createdDate <= :to) " +
           "ORDER BY d.createdDate DESC")
    Page<Debit> findByPlayerIdOrderByCreatedDateDesc(@Param("playerId") Integer playerId,
                                                       @Param("from") java.time.Instant from,
                                                       @Param("to") java.time.Instant to,
                                                       Pageable pageable);

    @Query("SELECT SUM(d.debtAmount) FROM Debit d WHERE d.player.playerId = :playerId")
    java.math.BigDecimal sumDebtAmountByPlayerId(@Param("playerId") Integer playerId);

    @Query("SELECT COUNT(d) FROM Debit d WHERE d.player.playerId = :playerId")
    long countDebtsByPlayerId(@Param("playerId") Integer playerId);

    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId AND d.status = 'PENDING' ORDER BY d.createdDate ASC")
    List<Debit> findPendingDebitsByPlayerIdOrderByCreatedDateAsc(@Param("playerId") Integer playerId);

    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId AND d.remainingAmount > 0 ORDER BY d.createdDate ASC")
    List<Debit> findUnpaidDebitsByPlayerIdOrderByCreatedDateAsc(@Param("playerId") Integer playerId);
}
