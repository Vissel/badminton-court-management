package com.badminton.repository;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
    Optional<Service> findBySerName(String serName);

    List<Service> findAllBySerName(String serName);

    List<Service> findAllByIsActive(boolean isActive);

    /**
     * Active catalog rows linked to one stock item (used to push Giá bán /
     * rename / deactivation back from Quản lý kho).
     */
    List<Service> findAllByItemAndIsActive(InventoryItem item, boolean isActive);
}
