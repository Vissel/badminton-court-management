package com.badminton.backup.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "backup_file")
public class BackupFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long fileId;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "trigger_id") private BackupTrigger trigger;
    private String schemaName;
    private String filePath;
    private Long fileSizeBytes;
    private Long rowCount;
    private String sha256;
    private Instant createdAt;
    protected BackupFile() {}
    public BackupFile(BackupTrigger trigger, String schemaName, String filePath, long size, long rows, String sha256) {
        this.trigger = trigger; this.schemaName = schemaName; this.filePath = filePath; this.fileSizeBytes = size;
        this.rowCount = rows; this.sha256 = sha256; this.createdAt = Instant.now();
    }
    public Long getFileId() { return fileId; }
    public String getSchemaName() { return schemaName; }
    public String getFilePath() { return filePath; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public Long getRowCount() { return rowCount; }
    public String getSha256() { return sha256; }
    public Instant getCreatedAt() { return createdAt; }
}
