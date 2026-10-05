package com.badminton.backup.model;

public record RabbitBackupRequest(String requestId, BackupScope scope) {}
