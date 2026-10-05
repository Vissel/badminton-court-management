package com.badminton.backup.repository;

import com.badminton.backup.entity.BackupWatermark;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupWatermarkRepository extends JpaRepository<BackupWatermark, Long> {
    Optional<BackupWatermark> findBySchemaNameAndTableName(String schemaName, String tableName);
}
