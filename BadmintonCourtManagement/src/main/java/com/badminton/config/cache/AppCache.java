package com.badminton.config.cache;

import com.badminton.model.CacheObject;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AppCache {
    // Thread-safe map
    private final Map<String, CacheObject> CACHE = new ConcurrentHashMap<>();

    public void put(String key, CacheObject value) {
        CACHE.put(key, value);
    }

    public Object get(String key) {
        CacheObject obj = CACHE.get(key);
        if (obj == null || obj.isExpired()) {
            CACHE.remove(key);
            return null;
        }
        return obj;
    }

    // Remove a key
    public void remove(String key) {
        CACHE.remove(key);
    }

    // Clear all cache
    public void clear() {
        CACHE.clear();
    }

    // Check existence
    public boolean contains(String key) {
        return CACHE.containsKey(key);
    }

    // For debugging / monitoring
    public Map<String, CacheObject> getAll() {
        return CACHE;
    }
}
