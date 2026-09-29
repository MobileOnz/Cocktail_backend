package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("활성 요청 저장소")
class ActiveRequestRegistryTest {

    @Test
    @DisplayName("30초를 넘긴 요청을 감지하고 완료 후 제거한다")
    void reportsLongRunningRequestAndStopsAfterCompletion() {
        // given
        ActiveRequestRegistry registry = new ActiveRequestRegistry();
        long startedAt = System.nanoTime();
        registry.start("trace-1234", "GET", "/slow", "127.0.0.1", startedAt);

        // when
        List<ActiveRequestRegistry.StuckRequest> stuck =
                registry.claimStuckRequests(
                        startedAt + TimeUnit.SECONDS.toNanos(31), 30_000, 60_000);

        // then
        assertThat(stuck).hasSize(1);
        assertThat(stuck.get(0).traceId()).isEqualTo("trace-1234");
        assertThat(stuck.get(0).elapsedMs()).isEqualTo(31_000);

        // when
        registry.complete("trace-1234");

        // then
        assertThat(
                        registry.claimStuckRequests(
                                startedAt + TimeUnit.MINUTES.toNanos(2), 30_000, 60_000))
                .isEmpty();
    }

    @Test
    @DisplayName("의도적으로 오래 유지되는 SSE 요청은 지연 요청으로 판단하지 않는다")
    void doesNotReportIntentionalEventStreamAsStuck() {
        // given
        ActiveRequestRegistry registry = new ActiveRequestRegistry();
        long startedAt = System.nanoTime();
        registry.start("trace-stream", "GET", "/stream", "127.0.0.1", startedAt);
        registry.markAsync("trace-stream", true);

        // when
        List<ActiveRequestRegistry.StuckRequest> stuck =
                registry.claimStuckRequests(
                        startedAt + TimeUnit.MINUTES.toNanos(10), 30_000, 60_000);

        // then
        assertThat(stuck).isEmpty();
    }

    @Test
    @DisplayName("같은 지연 요청의 경고를 설정된 반복 주기로 제한한다")
    void rateLimitsRepeatedWarnings() {
        // given
        ActiveRequestRegistry registry = new ActiveRequestRegistry();
        long startedAt = System.nanoTime();
        registry.start("trace-1234", "GET", "/slow", "127.0.0.1", startedAt);

        // when
        List<ActiveRequestRegistry.StuckRequest> firstWarning =
                registry.claimStuckRequests(
                        startedAt + TimeUnit.SECONDS.toNanos(31), 30_000, 60_000);
        List<ActiveRequestRegistry.StuckRequest> suppressedWarning =
                registry.claimStuckRequests(
                        startedAt + TimeUnit.SECONDS.toNanos(40), 30_000, 60_000);
        List<ActiveRequestRegistry.StuckRequest> repeatedWarning =
                registry.claimStuckRequests(
                        startedAt + TimeUnit.SECONDS.toNanos(92), 30_000, 60_000);

        // then
        assertThat(firstWarning).hasSize(1);
        assertThat(suppressedWarning).isEmpty();
        assertThat(repeatedWarning).hasSize(1);
    }
}
