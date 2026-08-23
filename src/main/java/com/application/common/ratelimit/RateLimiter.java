package com.application.common.ratelimit;

import java.time.Duration;

/**
 * 고정 윈도우 카운터. 단일 EC2 전제이므로 기본은 in-memory 이고,
 * onz.ratelimit.redis.enabled=true 일 때만 Redis 구현이 뜬다(다중 인스턴스 대비).
 */
public interface RateLimiter {

    /**
     * @return 허용되면 true. 한도 초과면 false.
     */
    boolean tryAcquire(String key, int limit, Duration window);
}
