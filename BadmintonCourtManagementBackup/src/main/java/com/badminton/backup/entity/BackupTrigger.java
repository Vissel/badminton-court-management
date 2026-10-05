package com.badminton.backup.entity;

import com.badminton.backup.model.BackupScope;
import com.badminton.backup.model.TriggerSource;
import com.badminton.backup.model.TriggerStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "backup_trigger")
public class BackupTrigger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long triggerId;
    @Enumerated(EnumType.STRING) private TriggerSource triggerSource;
    @Enumerated(EnumType.STRING) private BackupScope scope;
    @Enumerated(EnumType.STRING) private TriggerStatus status;
    private String scheduleType;
    private String externalRequestId;
    private Instant fromCutoff;
    private Instant toCutoff;
    private Instant startedAt;
    private Instant completedAt;
    @Column(length = 2000) private String errorMessage;
    private Instant createdAt;

    protected BackupTrigger() {}
    public BackupTrigger(TriggerSource source, BackupScope scope, String scheduleType, String externalRequestId) {
        this.triggerSource = source; this.scope = scope; this.scheduleType = scheduleType;
        this.externalRequestId = externalRequestId; this.status = TriggerStatus.PENDING; this.createdAt = Instant.now();
    }
    public Long getTriggerId() { return triggerId; }
    public TriggerSource getTriggerSource() { return triggerSource; }
    public BackupScope getScope() { return scope; }
    public TriggerStatus getStatus() { return status; }
    public String getScheduleType() { return scheduleType; }
    public String getExternalRequestId() { return externalRequestId; }
    public Instant getFromCutoff() { return fromCutoff; }
    public Instant getToCutoff() { return toCutoff; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void start(Instant from, Instant to) { status = TriggerStatus.RUNNING; startedAt = Instant.now(); fromCutoff = from; toCutoff = to; }
    public void complete() { status = TriggerStatus.COMPLETED; completedAt = Instant.now(); }
    public void fail(String error) { status = TriggerStatus.FAILED; completedAt = Instant.now(); errorMessage = error == null ? "Unknown failure" : error.substring(0, Math.min(error.length(), 2000)); }
}
