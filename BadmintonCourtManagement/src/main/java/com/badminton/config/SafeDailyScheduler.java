package com.badminton.config;

import com.badminton.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class SafeDailyScheduler {
    private final AtomicBoolean appReady = new AtomicBoolean(false);

    @Autowired
    SchedulerService schedulerService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        appReady.set(true);
        runDailyJob();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runDailyJob() {
        if (!appReady.get()) {
            log.warn("⏳ Skipping job — application not ready yet.");
            return;
        }

        log.info("✅ Running daily job after app startup: " + java.time.LocalDateTime.now());
        // Your job logic here — e.g. call service layer
        ResponseEntity responseEntity = ResponseEntity.ok(schedulerService.removeRedundantSession());
        log.info("===Scheduled task===, {}", responseEntity);
    }
}
