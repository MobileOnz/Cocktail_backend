package com.application.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Controller 레이어의 성능 로깅 및 요청 추적을 위한 AOP
 */
@Slf4j
@Aspect
@Component
public class ControllerLoggingAspect {

    private static final ThreadLocal<RequestContext> REQUEST_CONTEXT = new ThreadLocal<>();

    /**
     * ServiceLoggingAspect에서 요청 컨텍스트를 가져오기 위한 메서드
     */
    public static RequestContext getRequestContext() {
        return REQUEST_CONTEXT.get();
    }

    /**
     * RestController의 모든 메서드 실행 시 로깅
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestContext context = null;

        try {
            // HTTP 요청 정보 추출
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            String httpMethod = request.getMethod();
            String uri = request.getRequestURI();

            // Controller 정보 추출
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();

            // 시작 시간 기록
            long startTime = System.currentTimeMillis();

            // RequestContext 생성 및 ThreadLocal 저장
            context = RequestContext.builder()
                    .httpMethod(httpMethod)
                    .uri(uri)
                    .controllerName(className)
                    .controllerMethod(methodName)
                    .startTime(startTime)
                    .build();
            REQUEST_CONTEXT.set(context);

            // 시작 로그
            log.info("[{} {}] {}.{}() 시작", httpMethod, uri, className, methodName);

            // 실제 메서드 실행
            Object result = joinPoint.proceed();

            // 종료 시간 측정
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // 완료 로그
            log.info("[{} {}] {}.{}() 완료 - 총 {}ms",
                    httpMethod, uri, className, methodName, executionTime);

            return result;

        } catch (Exception e) {
            // 에러 발생 시에도 실행 시간 기록
            if (context != null) {
                long executionTime = System.currentTimeMillis() - context.getStartTime();
                log.error("[{} {}] {}.{}() 실패 - {}ms - {}",
                        context.getHttpMethod(),
                        context.getUri(),
                        context.getControllerName(),
                        context.getControllerMethod(),
                        executionTime,
                        e.getMessage());
            }
            throw e;

        } finally {
            // ThreadLocal 메모리 누수 방지 (필수!)
            REQUEST_CONTEXT.remove();
        }
    }
}
