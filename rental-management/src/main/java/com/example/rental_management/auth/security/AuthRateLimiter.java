package com.example.rental_management.auth.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Single-instance fixed-window limiter. Use a shared limiter or edge proxy when scaling out. */
@Component
public class AuthRateLimiter {
    static final int MAX_ENTRIES = 10_000;
    private final Map<String, Window> windows = new HashMap<>();

    public synchronized boolean allow(String key, int limit, Duration window, long nowMillis) {
        long windowMillis = window.toMillis();
        Window current = windows.get(key);
        if (current == null || nowMillis - current.startedAt >= windowMillis) {
            cleanup(nowMillis, windowMillis);
            if (windows.size() >= MAX_ENTRIES) return false;
            windows.put(key, new Window(nowMillis, 1));
            return true;
        }
        if (current.count >= limit) return false;
        current.count++;
        return true;
    }

    private void cleanup(long now, long ttl) {
        Iterator<Window> iterator = windows.values().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().startedAt >= ttl) iterator.remove();
        }
    }

    private static final class Window {
        private final long startedAt;
        private int count;
        private Window(long startedAt, int count) { this.startedAt = startedAt; this.count = count; }
    }
}
