package com.badminton.backup.model;

import jakarta.validation.constraints.NotNull;

public record BackupRequest(@NotNull BackupScope scope) {}
