package com.example.rental_management.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimiterTest {
    @Test
    void permitsRequestsBelowLimitAndBlocksAtLimit() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int i = 0; i < 10; i++) assertTrue(limiter.allow("login:127.0.0.1", 10, Duration.ofMinutes(15), 1_000));
        assertFalse(limiter.allow("login:127.0.0.1", 10, Duration.ofMinutes(15), 1_000));
    }

    @Test
    void expiresOldWindowAndKeepsOtherKeysIndependent() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        Duration window = Duration.ofMinutes(15);
        assertTrue(limiter.allow("change:ip:user", 1, window, 1_000));
        assertFalse(limiter.allow("change:ip:user", 1, window, 1_001));
        assertTrue(limiter.allow("change:other:user", 1, window, 1_001));
        assertTrue(limiter.allow("change:ip:user", 1, window, 901_000));
    }
}
