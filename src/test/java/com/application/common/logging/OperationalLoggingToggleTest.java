package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class OperationalLoggingToggleTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(LoggingConfiguration.class);

    @Test
    void enablesAllOperationalLoggingComponentsByDefault() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(ActiveRequestRegistry.class);
                    assertThat(context).hasSingleBean(HttpRequestLoggingFilter.class);
                    assertThat(context).hasSingleBean(MethodTraceLoggingAspect.class);
                    assertThat(context).hasSingleBean(StuckRequestWatchdog.class);
                });
    }

    @Test
    void disablesAllOperationalLoggingComponentsWithMasterSwitch() {
        contextRunner
                .withPropertyValues("app.logging.enabled=false")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(ActiveRequestRegistry.class);
                            assertThat(context).doesNotHaveBean(HttpRequestLoggingFilter.class);
                            assertThat(context).doesNotHaveBean(MethodTraceLoggingAspect.class);
                            assertThat(context).doesNotHaveBean(StuckRequestWatchdog.class);
                        });
    }

    @Test
    void disablesOnlyMethodTracingWithDetailedSwitch() {
        contextRunner
                .withPropertyValues(
                        "app.logging.enabled=true",
                        "app.logging.method-trace-enabled=false")
                .run(
                        context -> {
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
