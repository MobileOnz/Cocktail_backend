package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@DisplayName("운영 로그 설정 스위치")
class OperationalLoggingToggleTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(LoggingConfiguration.class);

    @Test
    @DisplayName("설정이 없으면 운영 로그 Bean을 기본 활성화한다")
    void enablesAllOperationalLoggingComponentsByDefault() {
        // given
        ApplicationContextRunner runner = contextRunner;

        // when
        runner.run(
                context -> {
                    // then: 로깅 Bean만 올리는 경량 컨텍스트이므로 DB에 연결하지 않는다.
                    assertThat(context).hasSingleBean(ActiveRequestRegistry.class);
                    assertThat(context).hasSingleBean(HttpRequestLoggingFilter.class);
                    assertThat(context).hasSingleBean(MethodTraceLoggingAspect.class);
                    assertThat(context).hasSingleBean(StuckRequestWatchdog.class);
                });
    }

    @Test
    @DisplayName("마스터 스위치가 OFF이면 모든 운영 로그 Bean을 비활성화한다")
    void disablesAllOperationalLoggingComponentsWithMasterSwitch() {
        // given
        ApplicationContextRunner runner =
                contextRunner.withPropertyValues("app.logging.enabled=false");

        // when
        runner.run(
                        context -> {
                            // then
                            assertThat(context).doesNotHaveBean(ActiveRequestRegistry.class);
                            assertThat(context).doesNotHaveBean(HttpRequestLoggingFilter.class);
                            assertThat(context).doesNotHaveBean(MethodTraceLoggingAspect.class);
                            assertThat(context).doesNotHaveBean(StuckRequestWatchdog.class);
                        });
    }

    @Test
    @DisplayName("세부 스위치가 OFF이면 HTTP 추적은 유지하고 메서드 AOP만 비활성화한다")
    void disablesOnlyMethodTracingWithDetailedSwitch() {
        // given
        ApplicationContextRunner runner =
                contextRunner.withPropertyValues(
                        "app.logging.enabled=true",
                        "app.logging.method-trace-enabled=false");

        // when
        runner.run(
                        context -> {
                            // then
                            assertThat(context).hasSingleBean(ActiveRequestRegistry.class);
                            assertThat(context).hasSingleBean(HttpRequestLoggingFilter.class);
                            assertThat(context).doesNotHaveBean(MethodTraceLoggingAspect.class);
                            assertThat(context).hasSingleBean(StuckRequestWatchdog.class);
                        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
        ActiveRequestRegistry.class,
        HttpRequestLoggingFilter.class,
        MethodTraceLoggingAspect.class,
        StuckRequestWatchdog.class
    })
    static class LoggingConfiguration {}
}
