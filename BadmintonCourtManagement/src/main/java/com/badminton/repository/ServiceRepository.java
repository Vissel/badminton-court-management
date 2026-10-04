package com.badminton.repository;

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
}
