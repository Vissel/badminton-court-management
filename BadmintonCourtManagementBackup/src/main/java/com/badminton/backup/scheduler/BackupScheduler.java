package com.badminton.backup.scheduler;

import com.badminton.backup.model.*;
import com.badminton.backup.service.BackupOrchestrator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BackupScheduler {
    private final BackupOrchestrator orchestrator;
    public BackupScheduler(BackupOrchestrator orchestrator) { this.orchestrator = orchestrator; }
    @Scheduled(cron = "${backup.schedule.daily-cron}", zone = "${backup.schedule.zone}")
    public void daily() { orchestrator.requestBackup(TriggerSource.SCHEDULED, BackupScope.INCREMENTAL, "DAILY", null); }
    @Scheduled(cron = "${backup.schedule.weekly-cron}", zone = "${backup.schedule.zone}")
    public void weekly() { orchestrator.requestBackup(TriggerSource.SCHEDULED, BackupScope.INCREMENTAL, "WEEKLY", null); }
    @Scheduled(cron = "${backup.schedule.monthly-cron}", zone = "${backup.schedule.zone}")
    public void monthly() { orchestrator.requestBackup(TriggerSource.SCHEDULED, BackupScope.FULL, "MONTHLY", null); }
}
