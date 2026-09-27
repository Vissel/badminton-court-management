package com.badminton.repository;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.PurchaseLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseLotRepository extends JpaRepository<PurchaseLot, Long> {

    List<PurchaseLot> findAllByItemOrderByPurchaseDateAsc(InventoryItem item);

    /**
     * Most recent lot: latest purchase_date; the newest lot wins ties.
     */
    Optional<PurchaseLot> findFirstByItemOrderByPurchaseDateDescLotIdDesc(InventoryItem item);
}
