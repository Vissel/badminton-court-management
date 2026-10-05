package com.badminton.backup.config;

import com.badminton.backup.service.BackupSourceDatabase;
import java.nio.file.Files;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class BackupHealthIndicator implements HealthIndicator {
    private final BackupProperties properties;
    private final BackupSourceDatabase source;
    public BackupHealthIndicator(BackupProperties properties, BackupSourceDatabase source) {
        this.properties = properties; this.source = source;
    }
    @Override
    public Health health() {
        try (var connection = source.connection()) {
            if (!connection.isValid(2)) return Health.down().withDetail("source", "invalid connection").build();
            if (!Files.isWritable(properties.storage().basePath()))
                return Health.down().withDetail("storage", "not writable").build();
            return Health.up().withDetail("source", properties.source().schema())
                    .withDetail("storage", properties.storage().basePath().toString()).build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
