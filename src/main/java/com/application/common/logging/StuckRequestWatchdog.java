package com.application.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 요청 처리 스레드를 막지 않고 별도 스케줄러에서 Registry를 조회해 장기 실행 요청을 경고한다.
 * 같은 요청은 설정된 반복 간격으로만 다시 보고해 로그 폭주를 막는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.logging.enabled",
        havingValue = "true",
        matchIfMissing = true)
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
            // 감시 스레드의 경고도 원래 요청 traceId로 검색되도록 잠시 MDC에 연결한다.
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
