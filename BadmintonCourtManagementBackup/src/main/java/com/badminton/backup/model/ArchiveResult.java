package com.badminton.backup.model;

import java.nio.file.Path;

public record ArchiveResult(Path path, long sizeBytes, long rowCount, String sha256) {}
