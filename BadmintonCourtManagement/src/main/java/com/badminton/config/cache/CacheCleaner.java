package com.badminton.config.cache;

import com.badminton.model.CacheObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheCleaner {
    private final AppCache cache;

    public CacheCleaner(AppCache cache) {
        this.cache = cache;
    }

    @Scheduled(fixedRate = 1800000) // every 180s
    public void cleanExpired() {
        cache.getAll().entrySet().removeIf(entry -> {
            Object value = entry.getValue();
            if (value instanceof CacheObject) {
                return ((CacheObject) value).isExpired();
            }
            return false;
        });
    }
}
