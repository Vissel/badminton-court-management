package com.badminton.backup.repository;

import com.badminton.backup.entity.BackupTrigger;
import com.badminton.backup.model.TriggerStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupTriggerRepository extends JpaRepository<BackupTrigger, Long> {
    Page<BackupTrigger> findAllByOrderByCreatedAtDesc(Pageable pageable);
    boolean existsByStatusIn(Collection<TriggerStatus> statuses);
}
