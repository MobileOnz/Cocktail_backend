package com.application.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 고유한 Request ID를 할당하고 MDC에 저장하는 필터
 * 로그 추적 및 디버깅을 위해 사용
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 요청 헤더에서 Request ID 확인, 없으면 새로 생성
            String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.trim().isEmpty()) {
                requestId = generateRequestId();
            }

            // MDC에 Request ID 저장 (로그에서 사용)
            MDC.put(REQUEST_ID_MDC_KEY, requestId);

            // 응답 헤더에 Request ID 추가 (프론트엔드에서 확인 가능)
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

            // 요청 정보 로깅
            log.info("요청 시작 - Method: {}, URI: {}, IP: {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    getClientIp(httpRequest));

            chain.doFilter(request, response);

            // 응답 상태 로깅
            log.info("요청 완료 - Status: {}", httpResponse.getStatus());

        } finally {
            // MDC 정리 (메모리 누수 방지)
            MDC.clear();
        }
    }

    /**
     * UUID 기반 고유 Request ID 생성
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 클라이언트의 실제 IP 주소 추출 (프록시/로드밸런서 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
