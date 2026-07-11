package com.application.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

@Slf4j
@Component
public class InMemoryChatEventBus implements ChatEventBus {

    private final Map<Long, Set<Consumer<ChatEvent>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void publish(long barId, ChatEvent event) {
        Set<Consumer<ChatEvent>> sinks = subscribers.get(barId);
        if (sinks == null || sinks.isEmpty()) return;

        for (Consumer<ChatEvent> sink : sinks) {
            try {
                sink.accept(event);
            } catch (Exception e) {
                // 한 구독자의 실패가 다른 구독자에게 전파되지 않도록 격리한다.
                log.debug("SSE sink 전송 실패 (연결 종료 추정): barId={} {}", barId, e.toString());
            }
        }
    }

    @Override
    public AutoCloseable subscribe(long barId, Consumer<ChatEvent> sink) {
        subscribers.computeIfAbsent(barId, k -> new CopyOnWriteArraySet<>()).add(sink);
        return () -> {
            Set<Consumer<ChatEvent>> sinks = subscribers.get(barId);
            if (sinks != null) {
                sinks.remove(sink);
                if (sinks.isEmpty()) subscribers.remove(barId, sinks);
            }
        };
    }

    public int subscriberCount(long barId) {
        Set<Consumer<ChatEvent>> s = subscribers.get(barId);
        return s == null ? 0 : s.size();
    }
}
