package com.badminton.backup.service;

import com.badminton.backup.entity.BackupTrigger;
import com.badminton.backup.entity.ProcessedBackupRequest;
import com.badminton.backup.model.*;
import com.badminton.backup.repository.BackupTriggerRepository;
import com.badminton.backup.repository.ProcessedBackupRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupOrchestrator {
    private final BackupTriggerRepository triggers;
    private final ProcessedBackupRequestRepository requests;
    private final ExecutionLockService locks;
    private final BackupRunner runner;
    public BackupOrchestrator(BackupTriggerRepository triggers, ProcessedBackupRequestRepository requests,
            ExecutionLockService locks, BackupRunner runner) {
        this.triggers = triggers; this.requests = requests; this.locks = locks; this.runner = runner;
    }

    public TriggerResponse requestBackup(TriggerSource source, BackupScope scope, String scheduleType, String requestId) {
        if (requestId != null) {
            var existing = requests.findById(requestId);
            if (existing.isPresent()) {
                BackupTrigger trigger = existing.get().getTrigger();
                return response(trigger);
            }
        }
        BackupTrigger trigger = triggers.save(new BackupTrigger(source, scope, scheduleType, requestId));
        try {
            locks.acquire(trigger.getTriggerId());
        } catch (RuntimeException exception) {
            trigger.fail(exception.getMessage());
            triggers.save(trigger);
            throw exception;
        }
        if (requestId != null) requests.save(new ProcessedBackupRequest(requestId, trigger));
        runner.run(trigger.getTriggerId());
        return response(trigger);
    }

    private TriggerResponse response(BackupTrigger trigger) {
        return new TriggerResponse(trigger.getTriggerId(), trigger.getStatus(), trigger.getScope());
    }
}
