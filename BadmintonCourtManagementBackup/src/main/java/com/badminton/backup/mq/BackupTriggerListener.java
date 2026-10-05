package com.badminton.backup.mq;

import com.badminton.backup.model.*;
import com.badminton.backup.service.BackupOrchestrator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "backup.rabbitmq", name = "enabled", havingValue = "true")
public class BackupTriggerListener {
    private final BackupOrchestrator orchestrator;
    public BackupTriggerListener(BackupOrchestrator orchestrator) { this.orchestrator = orchestrator; }
    @RabbitListener(queues = "${backup.rabbitmq.trigger-queue}")
    public void receive(RabbitBackupRequest request) {
        if (request.requestId() == null || request.requestId().isBlank() || request.requestId().length() > 100)
            throw new IllegalArgumentException("requestId must contain 1-100 characters");
        orchestrator.requestBackup(TriggerSource.RABBITMQ,
                request.scope() == null ? BackupScope.INCREMENTAL : request.scope(), null, request.requestId());
    }
}
