package com.badminton.repository;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.StockMovement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT COALESCE(SUM(m.quantityDelta), 0) FROM StockMovement m WHERE m.item = :item")
    long sumQuantityDeltaByItem(@Param("item") InventoryItem item);

    @Query("SELECT COALESCE(SUM(m.quantityDelta), 0) FROM StockMovement m WHERE m.item = :item AND m.movementType = :type")
    long sumQuantityDeltaByItemAndMovementType(@Param("item") InventoryItem item,
                                               @Param("type") com.badminton.enums.StockMovementType type);

    List<StockMovement> findAllByItemOrderByMovementIdAsc(InventoryItem item);

    List<StockMovement> findAllByItemAndCreatedDateBetweenOrderByMovementIdAsc(
            InventoryItem item, Timestamp from, Timestamp to);
}
