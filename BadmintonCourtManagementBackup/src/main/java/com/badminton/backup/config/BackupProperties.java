package com.badminton.backup.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backup")
public record BackupProperties(Source source, Storage storage, Schedule schedule, Rabbitmq rabbitmq, Security security) {
    public record Source(String url, String username, String password, String schema) {}
    public record Storage(Path basePath) {}
    public record Schedule(String zone, String dailyCron, String weeklyCron, String monthlyCron) {}
    public record Rabbitmq(boolean enabled, String exchange, String triggerQueue, String triggerRoutingKey,
            String completedRoutingKey, String failedRoutingKey) {}
    public record Security(String publicKeyLocation) {}
}
