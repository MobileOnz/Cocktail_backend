package com.application.domain.bar.dto.response;

import com.application.domain.bar.entity.BarChatMessage;

import java.time.LocalDateTime;

/** ⚠️ memberId 가 없다. authorRef 만으로 식별된다. */
public record ChatMessageDto(
        Long id,
        String authorRef,
        String nickname,
        String content,
        String status,
        LocalDateTime createdAt,
        boolean isMine
) {
    public static ChatMessageDto of(BarChatMessage m, String nickname, String viewerRef) {
        return new ChatMessageDto(m.getId(), m.getAuthorRef(), nickname, m.getContent(),
                m.getStatus(), m.getCreatedAt(), m.getAuthorRef().equals(viewerRef));
    }
}
