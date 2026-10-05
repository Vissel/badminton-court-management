package com.badminton.backup.config;

import com.badminton.backup.service.BackupCatalog;
import com.badminton.backup.service.BackupSourceDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BackupStartupValidator implements ApplicationRunner {
    private final BackupProperties properties;
    private final BackupSourceDatabase source;
    private final BackupCatalog catalog;
    public BackupStartupValidator(BackupProperties properties, BackupSourceDatabase source, BackupCatalog catalog) {
        this.properties = properties; this.source = source; this.catalog = catalog;
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path storage = properties.storage().basePath();
        Files.createDirectories(storage);
        if (!Files.isDirectory(storage) || !Files.isWritable(storage))
            throw new IllegalStateException("Backup storage is not writable: " + storage);
        try (var connection = source.connection()) { catalog.validate(connection); }
    }
}
