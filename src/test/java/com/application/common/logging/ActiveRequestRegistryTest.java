package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ActiveRequestRegistryTest {

    @Test
    void reportsLongRunningRequestAndStopsAfterCompletion() {
        ActiveRequestRegistry registry = new ActiveRequestRegistry();
        long startedAt = System.nanoTime();
        registry.start("trace-1234", "GET", "/slow", "127.0.0.1", startedAt);

        List<ActiveRequestRegistry.StuckRequest> stuck =
                registry.claimStuckRequests(
                        startedAt + TimeUnit.SECONDS.toNanos(31), 30_000, 60_000);

        assertThat(stuck).hasSize(1);
        assertThat(stuck.get(0).traceId()).isEqualTo("trace-1234");
        assertThat(stuck.get(0).elapsedMs()).isEqualTo(31_000);

        registry.complete("trace-1234");
        assertThat(
                        registry.claimStuckRequests(
                                startedAt + TimeUnit.MINUTES.toNanos(2), 30_000, 60_000))
                .isEmpty();
    }

    @Test
    void doesNotReportIntentionalEventStreamAsStuck() {
        ActiveRequestRegistry registry = new ActiveRequestRegistry();
        long startedAt = System.nanoTime();
        registry.start("trace-stream", "GET", "/stream", "127.0.0.1", startedAt);
        registry.markAsync("trace-stream", true);

        assertThat(
                        registry.claimStuckRequests(
                                startedAt + TimeUnit.MINUTES.toNanos(10), 30_000, 60_000))
                .isEmpty();
    }

    @Test
    void rateLimitsRepeatedWarnings() {
        ActiveRequestRegistry registry = new ActiveRequestRegistry();
        long startedAt = System.nanoTime();
        registry.start("trace-1234", "GET", "/slow", "127.0.0.1", startedAt);

        assertThat(
                        registry.claimStuckRequests(
                                startedAt + TimeUnit.SECONDS.toNanos(31), 30_000, 60_000))
                .hasSize(1);
        assertThat(
                        registry.claimStuckRequests(
                                startedAt + TimeUnit.SECONDS.toNanos(40), 30_000, 60_000))
                .isEmpty();
        assertThat(
                        registry.claimStuckRequests(
                                startedAt + TimeUnit.SECONDS.toNanos(92), 30_000, 60_000))
                .hasSize(1);
    }
}
