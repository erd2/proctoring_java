package com.aiu.proctoring.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting service using Redis sliding window.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    private static final String AI_REQUEST_KEY = "rate_limit:ai:%s";

    /**
     * Check if user is allowed to make AI request.
     * @param userId User identifier
     * @param limit Max requests per window
     * @param windowSec Window duration in seconds
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String userId, int limit, int windowSec) {
        String key = String.format(AI_REQUEST_KEY, userId);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            // Set expiry on first request
            redisTemplate.expire(key, Duration.ofSeconds(windowSec));
        }

        if (count != null && count > limit) {
            log.warn("Rate limit exceeded for user: {}", userId);
            return false;
        }

        return true;
    }

    /**
     * Get remaining quota for user.
     */
    public int getRemainingQuota(String userId, int limit, int windowSec) {
        String key = String.format(AI_REQUEST_KEY, userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count <= limit) {
            return limit - count.intValue();
        }
        return 0;
    }

    /**
     * Reset rate limit for user (for testing purposes).
     */
    public void reset(String userId) {
        String key = String.format(AI_REQUEST_KEY, userId);
        redisTemplate.delete(key);
    }
}
