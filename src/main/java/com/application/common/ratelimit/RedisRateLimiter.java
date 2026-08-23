package com.application.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * INCR + 최초 1회 EXPIRE. 다중 인스턴스에서만 필요하다.
 * Redis 장애 시 fail-open(허용)한다 — 레이트리밋 때문에 서비스가 죽는 것이 더 나쁘다.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redis;

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, window);
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            log.warn("RateLimiter fail-open (redis 장애): key={} {}", key, e.toString());
            return true;
        }
    }
}
