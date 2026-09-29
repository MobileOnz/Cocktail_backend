package com.application.common.logging;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * 한 스레드의 호출 스택으로 depth와 부모 span을 만들고 MDC에 반영한다.
 * 스택을 쓰는 이유는 자식 메서드가 끝난 뒤 부모의 로그 문맥을 정확히 복원하기 위해서다.
 */
final class MethodTraceContext {

    private static final String DEPTH_KEY = "depth";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String PARENT_SPAN_ID_KEY = "parentSpanId";
    private static final ThreadLocal<Deque<Span>> SPANS = ThreadLocal.withInitial(ArrayDeque::new);

    private MethodTraceContext() {}

    static Span open() {
        Deque<Span> stack = SPANS.get();
        Span parent = stack.peek();
        Span span =
                new Span(
                        UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                        parent == null ? "none" : parent.spanId(),
                        stack.size());
        stack.push(span);
        apply(span);
        return span;
    }

    static void close(Span span) {
        Deque<Span> stack = SPANS.get();
        // 정상적인 LIFO 종료가 아니어도 해당 span을 제거해 컨텍스트 누수를 방지한다.
        if (!stack.isEmpty() && stack.peek() == span) {
            stack.pop();
        } else {
            stack.remove(span);
        }

        Span parent = stack.peek();
        if (parent == null) {
            // 풀 스레드 재사용 시 이전 요청 정보가 남지 않도록 ThreadLocal과 MDC를 함께 정리한다.
            SPANS.remove();
            MDC.remove(DEPTH_KEY);
            MDC.remove(SPAN_ID_KEY);
            MDC.remove(PARENT_SPAN_ID_KEY);
        } else {
            apply(parent);
        }
    }

    static void clear() {
        SPANS.remove();
        MDC.remove(DEPTH_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove(PARENT_SPAN_ID_KEY);
    }

    private static void apply(Span span) {
        MDC.put(DEPTH_KEY, Integer.toString(span.depth()));
        MDC.put(SPAN_ID_KEY, span.spanId());
        MDC.put(PARENT_SPAN_ID_KEY, span.parentSpanId());
    }

    record Span(String spanId, String parentSpanId, int depth) {}
}
