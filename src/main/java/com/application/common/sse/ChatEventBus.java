package com.application.common.sse;

import java.util.function.Consumer;

/**
 * 바(bar) 단위 팬아웃. 단일 EC2 전제이므로 기본 구현은 in-memory 다.
 * 다중 인스턴스로 가면 이 인터페이스의 Redis Pub/Sub 구현으로 갈아끼운다 —
 * **갈아끼우는 지점은 여기 하나뿐이다.**
 */
public interface ChatEventBus {

    void publish(long barId, ChatEvent event);

    /** @return close() 하면 구독 해제. */
    AutoCloseable subscribe(long barId, Consumer<ChatEvent> sink);
}
