package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** 사용자가 특정 author_ref 를 차단. 재설치해도 유지된다(서버 저장). */
@Entity
@Table(name = "bar_chat_block")
@IdClass(BarChatBlock.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarChatBlock {

    @Id
    @Column(name = "blocker_ref", length = 32)
    private String blockerRef;

    @Id
    @Column(name = "blocked_ref", length = 32)
    private String blockedRef;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private BarChatBlock(String blockerRef, String blockedRef) {
        this.blockerRef = blockerRef;
        this.blockedRef = blockedRef;
        this.createdAt = LocalDateTime.now();
    }

    public static BarChatBlock of(String blockerRef, String blockedRef) {
        return new BarChatBlock(blockerRef, blockedRef);
    }

    public static class Key implements Serializable {
        private String blockerRef;
        private String blockedRef;

        public Key() {}

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(blockerRef, k.blockerRef) && Objects.equals(blockedRef, k.blockedRef);
        }
        @Override public int hashCode() { return Objects.hash(blockerRef, blockedRef); }
    }
}
