package com.application.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Service 레이어의 성능 로깅 및 메서드 호출 추적을 위한 AOP
 */
@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    /**
     * Service의 모든 메서드 실행 시 로깅
     */
    @Around("@within(org.springframework.stereotype.Service)")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        // ControllerLoggingAspect의 ThreadLocal에서 요청 컨텍스트 가져오기
        RequestContext context = ControllerLoggingAspect.getRequestContext();

        // Service 정보 추출
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 시작 시간 기록
        long startTime = System.currentTimeMillis();

        // 시작 로그
        if (context != null) {
            // Controller를 통한 호출인 경우 (들여쓰기로 계층 표시)
            log.info("  → {}.{}() 호출", className, methodName);
        } else {
            // Controller 없이 Service가 직접 호출된 경우
            log.info("[Direct] {}.{}() 호출", className, methodName);
        }

        try {
            // 실제 메서드 실행
            Object result = joinPoint.proceed();

            // 종료 시간 측정
            long executionTime = System.currentTimeMillis() - startTime;

            // 완료 로그
            if (context != null) {
                log.info("  → {}.{}() 완료 ({}ms)", className, methodName, executionTime);
            } else {
                log.info("[Direct] {}.{}() 완료 ({}ms)", className, methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            // 에러 발생 시에도 실행 시간 기록
            long executionTime = System.currentTimeMillis() - startTime;

            if (context != null) {
                log.error("  → {}.{}() 실패 ({}ms) - {}",
                        className, methodName, executionTime, e.getMessage());
            } else {
                log.error("[Direct] {}.{}() 실패 ({}ms) - {}",
                        className, methodName, executionTime, e.getMessage());
            }

            throw e;
        }
    }
}
