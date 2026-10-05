package com.badminton.backup.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "backup.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqConfig {
    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    DirectExchange backupExchange(BackupProperties p) {
        return new DirectExchange(p.rabbitmq().exchange(), true, false);
    }

    @Bean
    Queue backupTriggerQueue(BackupProperties p) {
        return new Queue(p.rabbitmq().triggerQueue(), true);
    }

    @Bean
    Binding backupTriggerBinding(Queue backupTriggerQueue, DirectExchange backupExchange, BackupProperties p) {
        return BindingBuilder.bind(backupTriggerQueue).to(backupExchange).with(p.rabbitmq().triggerRoutingKey());
    }
}
