package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class InMemoryRateLimitService implements RateLimitService {

    private final ConcurrentHashMap<String, RateLimitEntry> rateLimits = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, int maxAttempts, int windowSeconds) {
        RateLimitEntry entry = rateLimits.compute(key, (k, existing) -> {
            if (existing == null || existing.isWindowExpired()) {
                return new RateLimitEntry(windowSeconds);
            }
            return existing;
        });

        boolean allowed = entry.getCount() < maxAttempts;
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {} ({} attempts in {} seconds)",
                    key, entry.getCount(), windowSeconds);
        }
        return allowed;
    }

    @Override
    public void recordAttempt(String key) {
        rateLimits.compute(key, (k, existing) -> {
            if (existing == null || existing.isWindowExpired()) {
                RateLimitEntry entry = new RateLimitEntry(300); // Default 5 min window
                entry.increment();
                return entry;
            }
            existing.increment();
            return existing;
        });
    }

    @Override
    public int getRemainingAttempts(String key, int maxAttempts, int windowSeconds) {
        RateLimitEntry entry = rateLimits.compute(key, (k, existing) -> {
            if (existing == null || existing.isWindowExpired()) {
                return new RateLimitEntry(windowSeconds);
            }
            return existing;
        });

        return Math.max(0, maxAttempts - entry.getCount());
    }

    private static class RateLimitEntry {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        private final int windowSeconds;

        RateLimitEntry(int windowSeconds) {
            this.windowSeconds = windowSeconds;
            this.windowStart.set(System.currentTimeMillis());
        }

        void increment() {
            count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }

        boolean isWindowExpired() {
            long elapsed = System.currentTimeMillis() - windowStart.get();
            return elapsed > (windowSeconds * 1000L);
        }
    }
}
