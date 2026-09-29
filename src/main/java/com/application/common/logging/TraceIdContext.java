package com.application.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * 로그 호출부마다 ID를 전달하지 않아도 되도록 현재 요청의 traceId를 MDC에서 관리한다.
 *
 * <p>로그, 응답 헤더, 오류 응답이 같은 ID를 사용하게 하는 단일 진입점이며 풀 스레드 재사용에
 * 대비해 요청 종료 시 반드시 {@link #clear()}해야 한다.</p>
 */
public final class TraceIdContext {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER_NAME = "X-Trace-Id";

    private TraceIdContext() {}

    public static String create() {
        // UUID의 하이픈만 제거해 로그 검색이 쉬운 32자리 서버 ID로 사용한다.
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String get() {
        return MDC.get(MDC_KEY);
    }

    public static void set(String traceId) {
        MDC.put(MDC_KEY, traceId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    public static String getOrCreate() {
        String traceId = get();
        return traceId == null || traceId.isBlank() ? create() : traceId;
    }
}
