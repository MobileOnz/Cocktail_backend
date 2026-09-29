package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.application.domain.cocktail.controller.CocktailBookmarkController;
import com.application.domain.cocktail.repository.CocktailBookmarkRepository;
import com.application.domain.cocktail.service.CocktailBookmarkService;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class MethodTraceLoggingAspectTest {

    @AfterEach
    void clearContext() {
        MethodTraceContext.clear();
        MDC.clear();
    }

    @Test
    void logsControllerServiceRepositoryAsOneNestedTrace() throws Throwable {
        MethodTraceLoggingAspect aspect = new MethodTraceLoggingAspect();
        ProceedingJoinPoint repository =
                joinPoint(CocktailBookmarkRepository.class, "findById", () -> "result");
        ProceedingJoinPoint service =
                joinPoint(
                        CocktailBookmarkService.class,
                        "toggleBookmark",
                        () -> aspect.trace(repository));
        ProceedingJoinPoint controller =
                joinPoint(
                        CocktailBookmarkController.class,
                        "toggleBookmark",
                        () -> aspect.trace(service));

        Logger logger = (Logger) LoggerFactory.getLogger(MethodTraceLoggingAspect.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(aspect.trace(controller)).isEqualTo("result");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages)
                .anyMatch(message -> message.contains("layer=CONTROLLER") && message.contains("depth=0"))
                .anyMatch(message -> message.contains("layer=SERVICE") && message.contains("depth=1"))
                .anyMatch(message -> message.contains("layer=REPOSITORY") && message.contains("depth=2"));

        List<String> traceIds =
                appender.list.stream()
                        .map(event -> event.getMDCPropertyMap().get(TraceIdContext.MDC_KEY))
                        .distinct()
                        .toList();
        assertThat(traceIds).hasSize(1);
        assertThat(traceIds.get(0)).matches("[a-f0-9]{32}");
        assertThat(TraceIdContext.get()).isNull();
    }

    private static ProceedingJoinPoint joinPoint(
            Class<?> declaringType, String methodName, ThrowingSupplier action) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn(declaringType);
        when(joinPoint.proceed()).thenAnswer(invocation -> action.get());
        return joinPoint;
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws Throwable;
    }
}
