package com.badminton.repository;

import com.badminton.entity.InventoryItem;
import com.badminton.enums.InventoryItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Integer> {

    List<InventoryItem> findAllByIsActive(boolean isActive);

    List<InventoryItem> findAllByIsActiveAndItemType(boolean isActive, InventoryItemType itemType);

    Optional<InventoryItem> findByItemNameIgnoreCaseAndItemType(String itemName, InventoryItemType itemType);
}
