package com.badminton.backup.repository;

import com.badminton.backup.entity.BackupFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupFileRepository extends JpaRepository<BackupFile, Long> {
    List<BackupFile> findByTriggerTriggerIdOrderByFileId(long triggerId);
}
