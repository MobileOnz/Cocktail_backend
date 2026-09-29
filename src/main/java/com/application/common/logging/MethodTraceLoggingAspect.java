package com.application.common.logging;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Controller → Service → Repository 호출을 depth와 span 관계로 추적한다.
 *
 * <p>Spring AOP 프록시 경계를 추적하므로 같은 객체 안의 private 메서드나 self-invocation은
 * 자동 추적하지 않는다. 그런 지점이 운영상 중요하면 별도 Bean으로 분리하거나
 * {@link TraceOperation}을 Spring Bean의 public 메서드에 사용한다.</p>
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

    @Around(
            "(@within(org.springframework.web.bind.annotation.RestController)"
                    + " || @within(org.springframework.stereotype.Service)"
                    + " || @within(org.springframework.stereotype.Repository)"
                    + " || execution(public * com.application..repository..*(..))"
                    + " || @within(com.application.common.logging.TraceOperation)"
                    + " || @annotation(com.application.common.logging.TraceOperation))"
                    + " && !within(com.application.common.logging..*)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean ownsTraceId = TraceIdContext.get() == null;
        if (ownsTraceId) {
            TraceIdContext.set(TraceIdContext.create());
        }

        MethodTraceContext.Span span = MethodTraceContext.open();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String layer = resolveLayer(joinPoint);
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
            MethodTraceContext.close(span);
            if (ownsTraceId) {
                TraceIdContext.clear();
            }
        }
    }

    private static long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static String resolveLayer(ProceedingJoinPoint joinPoint) {
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
