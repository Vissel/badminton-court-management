package com.badminton.backup.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_backup_request")
public class ProcessedBackupRequest {
    @Id private String requestId;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "trigger_id", unique = true) private BackupTrigger trigger;
    private Instant createdAt;
    protected ProcessedBackupRequest() {}
    public ProcessedBackupRequest(String requestId, BackupTrigger trigger) { this.requestId = requestId; this.trigger = trigger; this.createdAt = Instant.now(); }
    public String getRequestId() { return requestId; }
    public BackupTrigger getTrigger() { return trigger; }
}
