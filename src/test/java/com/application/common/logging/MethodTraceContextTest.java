package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MethodTraceContextTest {

    @AfterEach
    void clearContext() {
        MethodTraceContext.clear();
        MDC.clear();
    }

    @Test
    void tracksDepthAndParentSpan() {
        MethodTraceContext.Span controller = MethodTraceContext.open();
        MethodTraceContext.Span service = MethodTraceContext.open();
        MethodTraceContext.Span repository = MethodTraceContext.open();

        assertThat(controller.depth()).isZero();
        assertThat(controller.parentSpanId()).isEqualTo("none");
        assertThat(service.depth()).isEqualTo(1);
        assertThat(service.parentSpanId()).isEqualTo(controller.spanId());
        assertThat(repository.depth()).isEqualTo(2);
        assertThat(repository.parentSpanId()).isEqualTo(service.spanId());
        assertThat(MDC.get("depth")).isEqualTo("2");

        MethodTraceContext.close(repository);
        assertThat(MDC.get("depth")).isEqualTo("1");
        assertThat(MDC.get("spanId")).isEqualTo(service.spanId());

        MethodTraceContext.close(service);
        MethodTraceContext.close(controller);
        assertThat(MDC.get("spanId")).isNull();
    }
}
