package com.application.common.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 완료되지 않은 HTTP 요청을 추적한다. */
@Component
@ConditionalOnProperty(
        name = "app.logging.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ActiveRequestRegistry {

    private final ConcurrentMap<String, ActiveRequest> activeRequests = new ConcurrentHashMap<>();

    public void start(String traceId, String method, String uri, String clientIp, long startedAtNanos) {
        activeRequests.put(
                traceId, new ActiveRequest(traceId, method, uri, clientIp, startedAtNanos));
    }

    public void markAsync(String traceId, boolean streaming) {
        ActiveRequest request = activeRequests.get(traceId);
        if (request != null) {
            request.kind = streaming ? RequestKind.STREAM : RequestKind.ASYNC;
        }
    }

    public void complete(String traceId) {
        activeRequests.remove(traceId);
    }

    public List<StuckRequest> claimStuckRequests(
            long nowNanos, long thresholdMs, long repeatIntervalMs) {
        long thresholdNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0, thresholdMs));
        long repeatNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(1, repeatIntervalMs));
        List<StuckRequest> result = new ArrayList<>();

        for (ActiveRequest request : activeRequests.values()) {
            if (request.kind == RequestKind.STREAM) {
                continue;
            }
            long elapsedNanos = nowNanos - request.startedAtNanos;
            if (elapsedNanos < thresholdNanos || !request.claimWarning(nowNanos, repeatNanos)) {
                continue;
            }
            result.add(
                    new StuckRequest(
                            request.traceId,
                            request.method,
                            request.uri,
                            request.clientIp,
                            request.kind,
                            TimeUnit.NANOSECONDS.toMillis(elapsedNanos)));
        }
        return result;
    }

    enum RequestKind {
        HTTP,
        ASYNC,
        STREAM
    }

    record StuckRequest(
            String traceId,
            String method,
            String uri,
            String clientIp,
            RequestKind kind,
            long elapsedMs) {}

    private static final class ActiveRequest {
        private final String traceId;
        private final String method;
        private final String uri;
        private final String clientIp;
        private final long startedAtNanos;
        private final AtomicLong lastWarningAtNanos = new AtomicLong();
        private volatile RequestKind kind = RequestKind.HTTP;

        private ActiveRequest(
                String traceId, String method, String uri, String clientIp, long startedAtNanos) {
            this.traceId = traceId;
            this.method = method;
            this.uri = uri;
            this.clientIp = clientIp;
            this.startedAtNanos = startedAtNanos;
        }

        private boolean claimWarning(long nowNanos, long repeatNanos) {
            while (true) {
                long last = lastWarningAtNanos.get();
                if (last != 0 && nowNanos - last < repeatNanos) {
                    return false;
                }
                if (lastWarningAtNanos.compareAndSet(last, nowNanos)) {
                    return true;
                }
            }
        }
    }
}
