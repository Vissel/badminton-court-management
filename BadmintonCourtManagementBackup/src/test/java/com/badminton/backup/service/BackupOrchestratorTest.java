package com.badminton.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.badminton.backup.entity.BackupTrigger;
import com.badminton.backup.entity.ProcessedBackupRequest;
import com.badminton.backup.model.*;
import com.badminton.backup.repository.BackupTriggerRepository;
import com.badminton.backup.repository.ProcessedBackupRequestRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BackupOrchestratorTest {
    private final BackupTriggerRepository triggers = mock(BackupTriggerRepository.class);
    private final ProcessedBackupRequestRepository requests = mock(ProcessedBackupRequestRepository.class);
    private final ExecutionLockService locks = mock(ExecutionLockService.class);
    private final BackupRunner runner = mock(BackupRunner.class);
    private final BackupOrchestrator orchestrator = new BackupOrchestrator(triggers, requests, locks, runner);

    @Test
    void duplicateRabbitRequestReturnsExistingTriggerWithoutRunningAgain() throws Exception {
        BackupTrigger trigger = trigger(41L, "request-1");
        when(requests.findById("request-1")).thenReturn(Optional.of(new ProcessedBackupRequest("request-1", trigger)));
        TriggerResponse response = orchestrator.requestBackup(TriggerSource.RABBITMQ, BackupScope.FULL, null, "request-1");
        assertThat(response.triggerId()).isEqualTo(41L);
        verifyNoInteractions(locks, runner);
    }

    @Test
    void newRequestAcquiresLockAndStartsSharedRunner() throws Exception {
        BackupTrigger saved = trigger(42L, null);
        when(triggers.save(any())).thenReturn(saved);
        TriggerResponse response = orchestrator.requestBackup(TriggerSource.REST, BackupScope.INCREMENTAL, null, null);
        assertThat(response.triggerId()).isEqualTo(42L);
        verify(locks).acquire(42L);
        verify(runner).run(42L);
    }

    private BackupTrigger trigger(long id, String requestId) throws Exception {
        BackupTrigger trigger = new BackupTrigger(TriggerSource.RABBITMQ, BackupScope.INCREMENTAL, null, requestId);
        Field field = BackupTrigger.class.getDeclaredField("triggerId"); field.setAccessible(true); field.set(trigger, id);
        return trigger;
    }
}
