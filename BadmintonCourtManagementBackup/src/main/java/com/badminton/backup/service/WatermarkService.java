package com.badminton.backup.service;

import com.badminton.backup.entity.BackupWatermark;
import com.badminton.backup.repository.BackupWatermarkRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatermarkService {
    private final BackupWatermarkRepository repository;
    public WatermarkService(BackupWatermarkRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Map<String, Instant> cutoffs(String schema, BackupCatalog catalog) {
        Map<String, Instant> result = new LinkedHashMap<>();
        for (BackupCatalog.Table table : catalog.tables())
            result.put(table.name(), repository.findBySchemaNameAndTableName(schema, table.name())
                    .map(BackupWatermark::getLastSuccessfulCutoff).orElse(Instant.EPOCH));
        return result;
    }

    @Transactional
    public void advanceAll(String schema, BackupCatalog catalog, Instant cutoff) {
        for (BackupCatalog.Table table : catalog.tables()) {
            BackupWatermark watermark = repository.findBySchemaNameAndTableName(schema, table.name())
                    .orElseGet(() -> new BackupWatermark(schema, table.name()));
            watermark.advance(cutoff);
            repository.save(watermark);
        }
    }
}
