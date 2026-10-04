package com.badminton.repository;

import com.badminton.entity.InventoryItem;
import com.badminton.enums.InventoryItemType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Integer> {

    List<InventoryItem> findAllByIsActive(boolean isActive);

    List<InventoryItem> findAllByIsActiveAndItemType(boolean isActive, InventoryItemType itemType);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<InventoryItem> findByItemNameIgnoreCaseAndItemType(String itemName, InventoryItemType itemType);
}
