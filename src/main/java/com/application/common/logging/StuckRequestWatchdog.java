package com.application.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 응답이 임계시간 안에 끝나지 않은 요청을 주기적으로 경고한다. */
@Slf4j
@Component
public class StuckRequestWatchdog {

    private final ActiveRequestRegistry registry;
    private final long thresholdMs;
    private final long repeatIntervalMs;

    public StuckRequestWatchdog(
            ActiveRequestRegistry registry,
            @Value("${app.logging.stuck-request-threshold-ms:30000}") long thresholdMs,
            @Value("${app.logging.stuck-request-repeat-ms:60000}") long repeatIntervalMs) {
        this.registry = registry;
        this.thresholdMs = Math.max(0, thresholdMs);
        this.repeatIntervalMs = Math.max(1, repeatIntervalMs);
    }

    @Scheduled(fixedDelayString = "${app.logging.stuck-request-scan-interval-ms:10000}")
    public void reportStuckRequests() {
        for (ActiveRequestRegistry.StuckRequest request :
                registry.claimStuckRequests(System.nanoTime(), thresholdMs, repeatIntervalMs)) {
            MDC.put(TraceIdContext.MDC_KEY, request.traceId());
            try {
                log.warn(
                        "HTTP_REQUEST_STILL_RUNNING method={} uri={} kind={} elapsedMs={} clientIp={}",
                        request.method(),
                        request.uri(),
                        request.kind(),
                        request.elapsedMs(),
                        request.clientIp());
            } finally {
                MDC.remove(TraceIdContext.MDC_KEY);
            }
        }
    }
}
