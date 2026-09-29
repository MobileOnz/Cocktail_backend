package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("장기 실행 요청 감시")
class StuckRequestWatchdogTest {

    @Test
    @DisplayName("임계 시간을 넘긴 요청은 ERROR 파일에서 보이도록 ERROR로 원래 TRACE_ID와 함께 기록한다")
    void reportsStuckRequestAsErrorWithOriginalTraceId() {
        // given: 1분 전에 시작해 아직 끝나지 않은 가짜 요청을 메모리 Registry에만 등록한다.
        ActiveRequestRegistry registry = new ActiveRequestRegistry();
        String traceId = TraceIdContext.create();
        long startedAt = System.nanoTime() - TimeUnit.MINUTES.toNanos(1);
        registry.start(traceId, "GET", "/api/v2/cocktails", "127.0.0.1", startedAt);
        StuckRequestWatchdog watchdog = new StuckRequestWatchdog(registry, 30_000, 60_000);

        Logger logger = (Logger) LoggerFactory.getLogger(StuckRequestWatchdog.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when
        try {
            watchdog.reportStuckRequests();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        // then
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .startsWith("HTTP_REQUEST_STILL_RUNNING")
                .contains("uri=/api/v2/cocktails", "kind=HTTP");
        assertThat(event.getMDCPropertyMap()).containsEntry(TraceIdContext.MDC_KEY, traceId);
        assertThat(TraceIdContext.get()).isNull();
    }
}
