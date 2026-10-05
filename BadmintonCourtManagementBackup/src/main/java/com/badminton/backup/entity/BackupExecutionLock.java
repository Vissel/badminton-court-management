package com.badminton.backup.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "backup_execution_lock")
public class BackupExecutionLock {
    @Id private Integer lockId;
    private boolean locked;
    private Long triggerId;
    protected BackupExecutionLock() {}
    public BackupExecutionLock(int lockId) { this.lockId = lockId; }
    public boolean isLocked() { return locked; }
    public Long getTriggerId() { return triggerId; }
    public void acquire(long id) { if (locked) throw new IllegalStateException("A backup is already running: " + triggerId); locked = true; triggerId = id; }
    public void release(long id) { if (triggerId != null && triggerId == id) { locked = false; triggerId = null; } }
}
