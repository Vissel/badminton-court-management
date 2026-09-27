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
import java.util.Optional;

@Repository
public interface DebitRepository extends JpaRepository<Debit, Integer> {

    List<Debit> findByPlayer_PlayerId(Integer playerId);

    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId ORDER BY d.createdDate DESC")
    List<Debit> findByPlayerIdOrderByCreatedDateDesc(@Param("playerId") Integer playerId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId " +
            "AND d.remainingAmount > 0 " +
            "AND (:from IS NULL OR d.createdDate >= :from) " +
            "AND (:to IS NULL OR d.createdDate <= :to) " +
            "ORDER BY d.createdDate DESC")
    Page<Debit> findByPlayerIdOrderByCreatedDateDesc(@Param("playerId") Integer playerId,
                                                     @Param("from") java.time.Instant from,
                                                     @Param("to") java.time.Instant to,
                                                     Pageable pageable);

    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId " +
            "AND (:from IS NULL OR d.createdDate >= :from) " +
            "AND (:to IS NULL OR d.createdDate <= :to) " +
            "AND (:amountFrom IS NULL OR d.debtAmount >= :amountFrom) " +
            "AND (:amountTo IS NULL OR d.debtAmount <= :amountTo) " +
            "ORDER BY d.createdDate DESC")
    Page<Debit> findHistoryByPlayerId(@Param("playerId") Integer playerId,
                                      @Param("from") java.time.Instant from,
                                      @Param("to") java.time.Instant to,
                                      @Param("amountFrom") java.math.BigDecimal amountFrom,
                                      @Param("amountTo") java.math.BigDecimal amountTo,
                                      Pageable pageable);

    /**
     * Aggregates over the full debit history: [0] total debt amount,
     * [1] total remaining amount, [2] total debit count, [3] fully-paid count.
     */
    @Query("SELECT COALESCE(SUM(d.debtAmount), 0), COALESCE(SUM(d.remainingAmount), 0), " +
            "COUNT(d), SUM(CASE WHEN d.remainingAmount = 0 THEN 1 ELSE 0 END) " +
            "FROM Debit d WHERE d.player.playerId = :playerId")
    Object[] summarizeHistoryByPlayerId(@Param("playerId") Integer playerId);

    @Query("SELECT SUM(d.debtAmount) FROM Debit d WHERE d.player.playerId = :playerId")
    java.math.BigDecimal sumDebtAmountByPlayerId(@Param("playerId") Integer playerId);

    @Query("SELECT COUNT(d) FROM Debit d WHERE d.player.playerId = :playerId")
    long countDebtsByPlayerId(@Param("playerId") Integer playerId);

    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId AND d.status = 'PENDING' ORDER BY d.createdDate ASC")
    List<Debit> findPendingDebitsByPlayerIdOrderByCreatedDateAsc(@Param("playerId") Integer playerId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT d FROM Debit d WHERE d.player.playerId = :playerId AND d.remainingAmount > 0 ORDER BY d.createdDate ASC")
    List<Debit> findUnpaidDebitsByPlayerIdOrderByCreatedDateAsc(@Param("playerId") Integer playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Debit d WHERE d.debitId = :debitId")
    Optional<Debit> findByIdForUpdate(@Param("debitId") Integer debitId);
}
