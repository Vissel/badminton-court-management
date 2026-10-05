package com.badminton.backup.mq;

import com.badminton.backup.config.BackupProperties;
import com.badminton.backup.entity.BackupTrigger;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "backup.rabbitmq", name = "enabled", havingValue = "true")
public class BackupStatusPublisher {
    private final RabbitTemplate rabbit;
    private final BackupProperties properties;
    public BackupStatusPublisher(RabbitTemplate rabbit, BackupProperties properties) { this.rabbit = rabbit; this.properties = properties; }
    public void publish(BackupTrigger trigger) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", trigger.getExternalRequestId()); payload.put("triggerId", trigger.getTriggerId());
        payload.put("status", trigger.getStatus()); payload.put("scope", trigger.getScope());
        payload.put("startedAt", trigger.getStartedAt()); payload.put("completedAt", trigger.getCompletedAt());
        payload.put("error", trigger.getErrorMessage());
        String key = trigger.getStatus().name().equals("COMPLETED") ? properties.rabbitmq().completedRoutingKey() : properties.rabbitmq().failedRoutingKey();
        rabbit.convertAndSend(properties.rabbitmq().exchange(), key, payload);
    }
}
