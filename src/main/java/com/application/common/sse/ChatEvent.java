package com.application.common.sse;

/**
 * SSE 로 브로드캐스트되는 이벤트.
 *
 * @param type    "message" | "hidden" | "presence"
 * @param eventId SSE `id:` 필드로 나가는 값. Last-Event-ID 재개의 기준.
 *                message 는 메시지 PK. 재개 커서가 아닌 이벤트(hidden 등)는 null.
 * @param payload data: 로 직렬화될 본문
 */
public record ChatEvent(String type, Long eventId, Object payload) {

    public static ChatEvent message(Long messageId, Object payload) {
        return new ChatEvent("message", messageId, payload);
    }

    public static ChatEvent hidden(Long messageId) {
        return new ChatEvent("hidden", null, new Hidden(messageId));
    }

    public record Hidden(Long id) {}
}
