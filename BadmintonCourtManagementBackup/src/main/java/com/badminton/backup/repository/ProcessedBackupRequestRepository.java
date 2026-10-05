package com.badminton.backup.repository;

import com.badminton.backup.entity.ProcessedBackupRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedBackupRequestRepository extends JpaRepository<ProcessedBackupRequest, String> {}
