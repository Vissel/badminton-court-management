package com.badminton.backup.service;

import com.badminton.backup.config.BackupProperties;
import com.badminton.backup.entity.BackupFile;
import com.badminton.backup.entity.BackupTrigger;
import com.badminton.backup.model.ArchiveResult;
import com.badminton.backup.mq.BackupStatusPublisher;
import com.badminton.backup.repository.BackupFileRepository;
import com.badminton.backup.repository.BackupTriggerRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BackupRunner {
    private static final Logger log = LoggerFactory.getLogger(BackupRunner.class);
    private final BackupTriggerRepository triggers;
    private final BackupFileRepository files;
    private final BackupExecutor executor;
    private final BackupSourceDatabase source;
    private final BackupCatalog catalog;
    private final WatermarkService watermarks;
    private final ExecutionLockService locks;
    private final BackupProperties properties;
    private final Optional<BackupStatusPublisher> publisher;

    public BackupRunner(BackupTriggerRepository triggers, BackupFileRepository files, BackupExecutor executor,
            BackupSourceDatabase source, BackupCatalog catalog, WatermarkService watermarks, ExecutionLockService locks,
            BackupProperties properties, Optional<BackupStatusPublisher> publisher) {
        this.triggers = triggers; this.files = files; this.executor = executor; this.source = source;
        this.catalog = catalog; this.watermarks = watermarks; this.locks = locks; this.properties = properties;
        this.publisher = publisher;
    }

    @Async
    public void run(long triggerId) {
        BackupTrigger trigger = triggers.findById(triggerId).orElseThrow();
        try {
            Instant cutoff = source.databaseTime();
            Map<String, Instant> from = watermarks.cutoffs(properties.source().schema(), catalog);
            trigger.start(trigger.getScope().name().equals("FULL") ? Instant.EPOCH : from.values().stream().min(Instant::compareTo).orElse(Instant.EPOCH), cutoff);
            triggers.save(trigger);
            ArchiveResult result = executor.execute(triggerId, trigger.getScope(), from, cutoff);
            files.save(new BackupFile(trigger, properties.source().schema(), result.path().toString(), result.sizeBytes(), result.rowCount(), result.sha256()));
            watermarks.advanceAll(properties.source().schema(), catalog, cutoff);
            trigger.complete();
            triggers.save(trigger);
            log.info("Backup {} completed: {}", triggerId, result.path());
        } catch (Exception exception) {
            trigger.fail(exception.getMessage());
            triggers.save(trigger);
            log.error("Backup {} failed", triggerId, exception);
        } finally {
            locks.release(triggerId);
            publisher.ifPresent(value -> value.publish(trigger));
        }
    }
}
