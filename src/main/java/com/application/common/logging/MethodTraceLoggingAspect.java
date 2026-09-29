package com.application.common.logging;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

/**
 * 각 메서드에 로깅 코드를 반복하지 않도록 AOP로 Controller → Service → Repository 흐름을 추적한다.
 *
 * <p>ThreadLocal 스택으로 depth와 부모 span을 만들고 MDC에 반영한다. Spring 프록시 경계만
 * 추적하므로 private 메서드나 self-invocation이 중요하면 별도 Bean으로 분리한다.</p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@ConditionalOnProperty(
        name = {"app.logging.enabled", "app.logging.method-trace-enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class MethodTraceLoggingAspect {

    private static final String APPLICATION_PACKAGE = "com.application.";

    // save/findById처럼 Spring Data 부모 인터페이스에 선언된 메서드는 패키지·어노테이션 조건에
    // 걸리지 않으므로 target()으로 Repository Bean 자체를 대상으로 삼는다.
    @Around(
            "(@within(org.springframework.web.bind.annotation.RestController)"
                    + " || @within(org.springframework.stereotype.Service)"
                    + " || @within(org.springframework.stereotype.Repository)"
                    + " || execution(public * com.application..repository..*(..))"
                    + " || target(org.springframework.data.repository.Repository)"
                    + " || @within(com.application.common.logging.TraceOperation)"
                    + " || @annotation(com.application.common.logging.TraceOperation))"
                    + " && !within(com.application.common.logging..*)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        // HTTP 요청 밖의 스케줄러·직접 호출도 독립적인 추적 단위로 검색할 수 있게 한다.
        boolean ownsTraceId = TraceIdContext.get() == null;
        if (ownsTraceId) {
            TraceIdContext.set(TraceIdContext.create());
        }

        MethodTraceContext.Span span = MethodTraceContext.open();
        Class<?> targetType = resolveTargetType(joinPoint);
        String className = targetType.getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String layer = resolveLayer(targetType, joinPoint);
        long startedAt = System.nanoTime();

        log.info(
                "METHOD_STARTED layer={} class={} method={} depth={} spanId={} parentSpanId={}",
                layer,
                className,
                methodName,
                span.depth(),
                span.spanId(),
                span.parentSpanId());

        try {
            Object result = joinPoint.proceed();
            log.info(
                    "METHOD_COMPLETED layer={} class={} method={} depth={} spanId={} durationMs={}",
                    layer,
                    className,
                    methodName,
                    span.depth(),
                    span.spanId(),
                    elapsedMs(startedAt));
            return result;
        } catch (Throwable failure) {
            // 같은 예외의 스택을 레이어마다 중복 출력하지 않고 최종 예외 핸들러에 맡긴다.
            log.warn(
                    "METHOD_FAILED layer={} class={} method={} depth={} spanId={} durationMs={} failureType={}",
                    layer,
                    className,
                    methodName,
                    span.depth(),
                    span.spanId(),
                    elapsedMs(startedAt),
                    failure.getClass().getSimpleName());
            throw failure;
        } finally {
            // 예외가 발생해도 부모 span을 복원해 이후 로그의 depth가 오염되지 않게 한다.
            MethodTraceContext.close(span);
            if (ownsTraceId) {
                TraceIdContext.clear();
            }
        }
    }

    private static long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static Class<?> resolveTargetType(ProceedingJoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        if (target == null) {
            return joinPoint.getSignature().getDeclaringType();
        }
        Class<?> targetClass = target.getClass();
        // Spring Data Repository는 JDK 프록시($ProxyNNN)라 실제로 선언한 우리 인터페이스 이름을 찾는다.
        if (Proxy.isProxyClass(targetClass)) {
            for (Class<?> type : targetClass.getInterfaces()) {
                if (type.getName().startsWith(APPLICATION_PACKAGE)) {
                    return type;
                }
            }
        }
        return ClassUtils.getUserClass(targetClass);
    }

    private static String resolveLayer(Class<?> targetType, ProceedingJoinPoint joinPoint) {
        // 패키지 위치와 무관하게 분류되도록 Bean에 붙은 스테레오타입을 먼저 본다.
        if (AnnotatedElementUtils.hasAnnotation(targetType, Controller.class)) {
            return "CONTROLLER";
        }
        if (AnnotatedElementUtils.hasAnnotation(targetType, Service.class)) {
            return "SERVICE";
        }
        if (AnnotatedElementUtils.hasAnnotation(targetType, Repository.class)
                || org.springframework.data.repository.Repository.class.isAssignableFrom(targetType)) {
            return "REPOSITORY";
        }

        String packageName = joinPoint.getSignature().getDeclaringType().getPackageName();
        if (packageName.contains(".controller")) {
            return "CONTROLLER";
        }
        if (packageName.contains(".service")) {
            return "SERVICE";
        }
        if (packageName.contains(".repository")) {
            return "REPOSITORY";
        }
        return "OPERATION";
    }
}
