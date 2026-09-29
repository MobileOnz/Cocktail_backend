package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.application.domain.cocktail.controller.CocktailBookmarkController;
import com.application.domain.cocktail.entity.CocktailBookmark;
import com.application.domain.cocktail.repository.CocktailBookmarkRepository;
import com.application.domain.cocktail.service.CocktailBookmarkService;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@DisplayName("메서드 호출 추적 AOP")
class MethodTraceLoggingAspectTest {

    @AfterEach
    void clearContext() {
        MethodTraceContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("Mock 호출을 Controller-Service-Repository 순서와 하나의 TRACE_ID로 기록한다")
    void logsControllerServiceRepositoryAsOneNestedTrace() throws Throwable {
        // given: 실제 Bean과 DB 대신 Mockito로 가짜 호출 체인을 구성한다.
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

        // when
        try {
            assertThat(aspect.trace(controller)).isEqualTo("result");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        // then
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

    @Test
    @DisplayName("Spring Data Repository 프록시는 $Proxy 대신 선언한 Repository 인터페이스 이름으로 기록한다")
    void logsRepositoryInterfaceNameInsteadOfJdkProxyName() throws Throwable {
        // given: Spring Data가 만드는 것과 같은 JDK 동적 프록시를 DB 없이 만든다.
        MethodTraceLoggingAspect aspect = new MethodTraceLoggingAspect();
        Object repositoryProxy =
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] {CocktailBookmarkRepository.class},
                        (proxy, method, args) -> null);
        ProceedingJoinPoint repository =
                joinPoint(repositoryProxy, CrudRepository.class, "findById", () -> "result");

        // when
        List<String> messages = captureMessages(() -> aspect.trace(repository));

        // then
        assertThat(messages)
                .allMatch(message -> message.contains("layer=REPOSITORY"))
                .allMatch(message -> message.contains("class=CocktailBookmarkRepository"))
                .noneMatch(message -> message.contains("$Proxy"));
    }

    @Test
    @DisplayName("Spring Data 부모 인터페이스에서 상속한 save·findById 호출도 추적한다")
    void tracesInheritedSpringDataRepositoryMethods() throws Throwable {
        // given: Spring Data처럼 JDK 프록시 Repository를 만들고 실제 Pointcut 매칭으로 AOP를 적용한다.
        Object repositoryTarget =
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[] {CocktailBookmarkRepository.class},
                        (proxy, method, args) ->
                                method.getReturnType() == Optional.class ? Optional.empty() : null);
        AspectJProxyFactory factory = new AspectJProxyFactory(repositoryTarget);
        factory.addAspect(new MethodTraceLoggingAspect());
        CocktailBookmarkRepository repository = factory.getProxy();
        CocktailBookmark bookmark = mock(CocktailBookmark.class);

        // when
        List<String> messages =
                captureMessages(
                        () -> {
                            repository.findById(1L);
                            repository.save(bookmark);
                            return repository.findByMemberIdAndCocktailId(1L, 2L);
                        });

        // then
        assertThat(messages)
                .anyMatch(message -> message.contains("METHOD_COMPLETED") && message.contains("method=findById"))
                .anyMatch(message -> message.contains("METHOD_COMPLETED") && message.contains("method=save"))
                .anyMatch(
                        message ->
                                message.contains("METHOD_COMPLETED")
                                        && message.contains("method=findByMemberIdAndCocktailId"))
                .filteredOn(message -> message.startsWith("METHOD_"))
                .allMatch(
                        message ->
                                message.contains("layer=REPOSITORY")
                                        && message.contains("class=CocktailBookmarkRepository"));
    }

    @Test
    @DisplayName("service 패키지 밖에 있어도 @Service Bean은 SERVICE 레이어로 분류한다")
    void classifiesServiceAnnotatedBeanOutsideServicePackageAsService() throws Throwable {
        // given: com.application.common.logging 패키지라 패키지 이름만으로는 SERVICE가 아니다.
        MethodTraceLoggingAspect aspect = new MethodTraceLoggingAspect();
        ProceedingJoinPoint service =
                joinPoint(
                        new OutsideServicePackageService(),
                        OutsideServicePackageService.class,
                        "check",
                        () -> "result");

        // when
        List<String> messages = captureMessages(() -> aspect.trace(service));

        // then
        assertThat(messages)
                .allMatch(message -> message.contains("layer=SERVICE"))
                .allMatch(message -> message.contains("class=OutsideServicePackageService"));
    }

    private static List<String> captureMessages(ThrowingSupplier action) throws Throwable {
        Logger logger = (Logger) LoggerFactory.getLogger(MethodTraceLoggingAspect.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.get();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private static ProceedingJoinPoint joinPoint(
            Class<?> declaringType, String methodName, ThrowingSupplier action) throws Throwable {
        return joinPoint(new Object(), declaringType, methodName, action);
    }

    private static ProceedingJoinPoint joinPoint(
            Object target, Class<?> declaringType, String methodName, ThrowingSupplier action)
            throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getTarget()).thenReturn(target);
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

    @Service
    static class OutsideServicePackageService {}
}
