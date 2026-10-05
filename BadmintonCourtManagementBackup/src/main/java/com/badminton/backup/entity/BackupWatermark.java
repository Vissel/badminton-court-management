package com.badminton.backup.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "backup_watermark", uniqueConstraints = @UniqueConstraint(name = "uk_watermark_schema_table", columnNames = {"schema_name", "table_name"}))
public class BackupWatermark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long watermarkId;
    private String schemaName;
    private String tableName;
    private Instant lastSuccessfulCutoff;
    private Instant updatedAt;
    protected BackupWatermark() {}
    public BackupWatermark(String schemaName, String tableName) { this.schemaName = schemaName; this.tableName = tableName; }
    public Long getWatermarkId() { return watermarkId; }
    public String getSchemaName() { return schemaName; }
    public String getTableName() { return tableName; }
    public Instant getLastSuccessfulCutoff() { return lastSuccessfulCutoff; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void advance(Instant cutoff) { lastSuccessfulCutoff = cutoff; updatedAt = Instant.now(); }
}
