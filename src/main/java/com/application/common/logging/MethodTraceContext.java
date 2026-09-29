package com.application.common.logging;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import org.slf4j.MDC;

/** 한 실행 스레드 안의 메서드 호출 깊이와 부모-자식 span 관계를 관리한다. */
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
        if (!stack.isEmpty() && stack.peek() == span) {
            stack.pop();
        } else {
            stack.remove(span);
        }

        Span parent = stack.peek();
        if (parent == null) {
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
