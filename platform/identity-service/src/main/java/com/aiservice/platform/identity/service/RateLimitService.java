package com.aiservice.platform.identity.service;

public interface RateLimitService {

    boolean isAllowed(String key, int maxAttempts, int windowSeconds);

    void recordAttempt(String key);

    int getRemainingAttempts(String key, int maxAttempts, int windowSeconds);
}
