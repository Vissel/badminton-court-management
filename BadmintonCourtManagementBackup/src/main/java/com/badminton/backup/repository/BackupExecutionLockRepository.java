package com.badminton.backup.repository;

import com.badminton.backup.entity.BackupExecutionLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface BackupExecutionLockRepository extends JpaRepository<BackupExecutionLock, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from BackupExecutionLock l where l.lockId = 1")
    BackupExecutionLock lockForUpdate();
}
