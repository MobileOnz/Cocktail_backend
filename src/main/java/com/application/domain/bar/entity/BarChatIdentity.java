package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * author_ref ↔ member_id 매핑. **신고 처리를 위한 유일한 역추적 창구**이며 30일 후 파기된다.
 * 파기 이후 메시지는 수학적으로 익명이다(HMAC 역산 불가 + 매핑표 소멸).
 */
@Entity
@Table(name = "bar_chat_identity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarChatIdentity {

    @Id
    @Column(name = "author_ref", length = 32)
    private String authorRef;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "bar_id", nullable = false)
    private Long barId;

    @Column(name = "epoch_date", nullable = false)
    private LocalDate epochDate;

    @Column(name = "nickname", nullable = false, length = 60)
    private String nickname;

    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private BarChatIdentity(String authorRef, Long memberId, Long barId, LocalDate epochDate, String nickname) {
        this.authorRef = authorRef;
        this.memberId = memberId;
        this.barId = barId;
        this.epochDate = epochDate;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
    }

    public static BarChatIdentity of(String authorRef, Long memberId, Long barId, LocalDate epoch, String nickname) {
        return new BarChatIdentity(authorRef, memberId, barId, epoch, nickname);
    }

    public void mute(LocalDateTime until) {
        this.mutedUntil = until;
    }

    public boolean isMuted(LocalDateTime now) {
        return mutedUntil != null && mutedUntil.isAfter(now);
    }
}
