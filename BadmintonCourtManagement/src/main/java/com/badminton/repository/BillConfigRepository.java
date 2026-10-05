package com.badminton.repository;

import com.badminton.entity.BillConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillConfigRepository extends JpaRepository<BillConfig, Integer> {

    /** The config table holds a single row — the first one. */
    Optional<BillConfig> findFirstByOrderByConfigIdAsc();
}
