package com.application.common.ratelimit;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 단일 인스턴스용 고정 윈도우. 윈도우가 지나면 카운터를 리셋한다.
 * 만료된 엔트리는 접근 시 정리되고, 주기적 스윕은 ChatRetentionScheduler 가 하지 않는다
 * (키 수가 회원×바 규모로 제한적이므로 별도 GC 불필요).
 */
@Slf4j
public class InMemoryRateLimiter implements RateLimiter {

    private record Window(AtomicInteger count, long resetAtMillis) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        Window w = windows.compute(key, (k, cur) ->
                (cur == null || now >= cur.resetAtMillis())
                        ? new Window(new AtomicInteger(0), now + window.toMillis())
                        : cur);
        return w.count().incrementAndGet() <= limit;
    }
}
