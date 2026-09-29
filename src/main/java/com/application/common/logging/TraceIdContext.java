package com.application.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * 현재 요청의 추적 ID를 MDC에서 관리한다.
 *
 * <p>로그와 오류 응답이 같은 ID를 사용하게 하는 단일 진입점이다.</p>
 */
public final class TraceIdContext {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER_NAME = "X-Trace-Id";

    private TraceIdContext() {}

    public static String create() {
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
