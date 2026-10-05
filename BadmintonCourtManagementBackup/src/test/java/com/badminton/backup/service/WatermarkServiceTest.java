package com.badminton.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.badminton.backup.entity.BackupWatermark;
import com.badminton.backup.repository.BackupWatermarkRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WatermarkServiceTest {
    @Test
    void missingWatermarksStartAtEpoch() {
        BackupWatermarkRepository repository = mock(BackupWatermarkRepository.class);
        when(repository.findBySchemaNameAndTableName(anyString(), anyString())).thenReturn(Optional.empty());
        var cutoffs = new WatermarkService(repository).cutoffs("badminton-db", new BackupCatalog());
        assertThat(cutoffs).hasSize(25).allSatisfy((table, cutoff) -> assertThat(cutoff).isEqualTo(Instant.EPOCH));
    }

    @Test
    void advancesEveryCatalogTableOnlyAfterRequested() {
        BackupWatermarkRepository repository = mock(BackupWatermarkRepository.class);
        when(repository.findBySchemaNameAndTableName(anyString(), anyString())).thenReturn(Optional.empty());
        Instant cutoff = Instant.parse("2026-10-05T12:00:00Z");
        new WatermarkService(repository).advanceAll("badminton-db", new BackupCatalog(), cutoff);
        verify(repository, times(25)).save(any(BackupWatermark.class));
    }
}
