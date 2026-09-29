package com.application.common.logging;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Controller 이전의 인증·파싱 실패까지 놓치지 않기 위해 가장 앞단의 Filter에서 요청을 추적한다.
 *
 * <p>서버가 만든 traceId를 MDC와 응답 헤더에 넣고, 활성 요청 Registry로 시작부터 실제 응답
 * 완료까지 연결한다. 본문, 쿼리 문자열, Authorization/Cookie는 민감정보 보호를 위해 기록하지 않는다.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
        name = "app.logging.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_URI_LENGTH = 2_048;

    private final ActiveRequestRegistry activeRequestRegistry;
    private final long slowRequestThresholdMs;

    public HttpRequestLoggingFilter(
            ActiveRequestRegistry activeRequestRegistry,
            @Value("${app.logging.slow-request-threshold-ms:1000}") long slowRequestThresholdMs) {
        this.activeRequestRegistry = activeRequestRegistry;
        this.slowRequestThresholdMs = Math.max(0, slowRequestThresholdMs);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 외부 ID를 신뢰하지 않고 서버에서 생성해 로그 위조와 요청 간 혼선을 막는다.
        String traceId = TraceIdContext.create();
        String method = sanitize(request.getMethod(), 16);
        String uri = sanitize(request.getRequestURI(), MAX_URI_LENGTH);
        String clientIp = sanitize(request.getRemoteAddr(), 64);
        long startedAt = System.nanoTime();
        Throwable failure = null;

        TraceIdContext.set(traceId);
        activeRequestRegistry.start(traceId, method, uri, clientIp, startedAt);

        try {
            response.setHeader(TraceIdContext.HEADER_NAME, traceId);
            log.info("HTTP_REQUEST_STARTED method={} uri={} clientIp={}", method, uri, clientIp);
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error ex) {
            failure = ex;
            throw ex;
        } finally {
            if (failure == null && request.isAsyncStarted()) {
                // 비동기는 Filter 반환 시점이 응답 완료가 아니므로 Listener가 최종 시간을 기록한다.
                boolean streaming = isEventStream(response);
                activeRequestRegistry.markAsync(traceId, streaming);
                if (registerAsyncCompletion(
                        request, response, traceId, method, uri, clientIp, startedAt)) {
                    log.info(
                            "HTTP_REQUEST_ASYNC_STARTED method={} uri={} kind={}",
                            method,
                            uri,
                            streaming ? "STREAM" : "ASYNC");
                } else {
                    completeRequest(
                            response, traceId, method, uri, clientIp, startedAt, null, "COMPLETED");
                }
            } else {
                completeRequest(
                        response, traceId, method, uri, clientIp, startedAt, failure, "COMPLETED");
            }
            // Tomcat 스레드는 재사용되므로 다음 요청에 MDC/호출 스택이 섞이지 않게 반드시 비운다.
            MethodTraceContext.clear();
            TraceIdContext.clear();
        }
    }

    private boolean registerAsyncCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            String traceId,
            String method,
            String uri,
            String clientIp,
            long startedAt) {
        try {
            request.getAsyncContext()
                    .addListener(
                            new RequestAsyncListener(
                                    response, traceId, method, uri, clientIp, startedAt));
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private void completeRequest(
            HttpServletResponse response,
            String traceId,
            String method,
            String uri,
            String clientIp,
            long startedAt,
            Throwable failure,
            String terminalEvent) {
        activeRequestRegistry.complete(traceId);
        // Async callback은 다른 스레드에서 실행될 수 있어 완료 로그 직전에 traceId를 복원한다.
        TraceIdContext.set(traceId);
        try {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            int status = failure == null ? response.getStatus() : effectiveFailureStatus(response.getStatus());
            if ("TIMEOUT".equals(terminalEvent) && status < 400) {
                status = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            } else if (!"COMPLETED".equals(terminalEvent) && status < 400) {
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            writeCompletionLog(method, uri, clientIp, status, durationMs, failure, terminalEvent);
        } finally {
            TraceIdContext.clear();
        }
    }

    private void writeCompletionLog(
            String method,
            String uri,
            String clientIp,
            int status,
            long durationMs,
            Throwable failure,
            String terminalEvent) {
        String message =
                "HTTP_REQUEST_"
                        + terminalEvent
                        + " method={} uri={} status={} durationMs={} clientIp={}";

        if (failure != null) {
            log.error(
                    message + " failureType={}",
                    method,
                    uri,
                    status,
                    durationMs,
                    clientIp,
                    failure.getClass().getSimpleName(),
                    failure);
        } else if (status >= 500 || !"COMPLETED".equals(terminalEvent)) {
            log.error(message, method, uri, status, durationMs, clientIp);
        } else if (durationMs >= slowRequestThresholdMs) {
            log.warn(message, method, uri, status, durationMs, clientIp);
        } else {
            log.info(message, method, uri, status, durationMs, clientIp);
        }
    }

    private static int effectiveFailureStatus(int responseStatus) {
        return responseStatus >= 400 ? responseStatus : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private static boolean isEventStream(HttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("text/event-stream");
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) {
            return "-";
        }
        // 개행 삽입과 비정상적으로 긴 값으로 로그 형식이 깨지는 것을 막는다.
        String singleLine = value.replace('\r', '_').replace('\n', '_');
        return singleLine.length() <= maxLength ? singleLine : singleLine.substring(0, maxLength);
    }

    private final class RequestAsyncListener implements AsyncListener {

        private final HttpServletResponse response;
        private final String traceId;
        private final String method;
        private final String uri;
        private final String clientIp;
        private final long startedAt;
        private final AtomicBoolean finished = new AtomicBoolean();

        private RequestAsyncListener(
                HttpServletResponse response,
                String traceId,
                String method,
                String uri,
                String clientIp,
                long startedAt) {
            this.response = response;
            this.traceId = traceId;
            this.method = method;
            this.uri = uri;
            this.clientIp = clientIp;
            this.startedAt = startedAt;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            finish(null, "COMPLETED");
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            finish(event.getThrowable(), "TIMEOUT");
        }

        @Override
        public void onError(AsyncEvent event) {
            finish(event.getThrowable(), "FAILED");
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }

        private void finish(Throwable failure, String terminalEvent) {
            // 완료·타임아웃·오류 callback이 경쟁해도 종료 로그는 한 번만 남긴다.
            if (finished.compareAndSet(false, true)) {
                completeRequest(
                        response,
                        traceId,
                        method,
                        uri,
                        clientIp,
                        startedAt,
                        failure,
                        terminalEvent);
            }
        }
    }
}
