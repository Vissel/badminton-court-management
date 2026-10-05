package com.badminton.backup.service;

import com.badminton.backup.config.BackupProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class BackupSourceDatabase {
    private final HikariDataSource dataSource;

    public BackupSourceDatabase(BackupProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.source().url());
        config.setUsername(properties.source().username());
        config.setPassword(properties.source().password());
        config.setPoolName("bcm-backup-read-only");
        config.setReadOnly(true);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10_000);
        dataSource = new HikariDataSource(config);
    }

    public Connection connection() throws SQLException { return dataSource.getConnection(); }

    public Instant databaseTime() throws SQLException {
        try (Connection connection = connection(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT CURRENT_TIMESTAMP(6)")) {
            result.next();
            return result.getTimestamp(1).toInstant();
        }
    }

    @PreDestroy
    void close() { dataSource.close(); }
}
