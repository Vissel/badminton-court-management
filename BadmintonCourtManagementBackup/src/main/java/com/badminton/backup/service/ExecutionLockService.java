package com.badminton.backup.service;

import com.badminton.backup.entity.BackupExecutionLock;
import com.badminton.backup.repository.BackupExecutionLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionLockService {
    private final BackupExecutionLockRepository repository;
    public ExecutionLockService(BackupExecutionLockRepository repository) { this.repository = repository; }
    @Transactional public void acquire(long triggerId) { repository.lockForUpdate().acquire(triggerId); }
    @Transactional public void release(long triggerId) { repository.lockForUpdate().release(triggerId); }
}
