package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("메서드 호출 추적 컨텍스트")
class MethodTraceContextTest {

    @AfterEach
    void clearContext() {
        MethodTraceContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("중첩 호출의 depth와 부모 span을 만들고 종료 시 부모 문맥을 복원한다")
    void tracksDepthAndParentSpan() {
        // given & when
        MethodTraceContext.Span controller = MethodTraceContext.open();
        MethodTraceContext.Span service = MethodTraceContext.open();
        MethodTraceContext.Span repository = MethodTraceContext.open();

        // then
        assertThat(controller.depth()).isZero();
        assertThat(controller.parentSpanId()).isEqualTo("none");
        assertThat(service.depth()).isEqualTo(1);
        assertThat(service.parentSpanId()).isEqualTo(controller.spanId());
        assertThat(repository.depth()).isEqualTo(2);
        assertThat(repository.parentSpanId()).isEqualTo(service.spanId());
        assertThat(MDC.get("depth")).isEqualTo("2");

        // when
        MethodTraceContext.close(repository);

        // then
        assertThat(MDC.get("depth")).isEqualTo("1");
        assertThat(MDC.get("spanId")).isEqualTo(service.spanId());

        // when
        MethodTraceContext.close(service);
        MethodTraceContext.close(controller);

        // then
        assertThat(MDC.get("spanId")).isNull();
    }
}
