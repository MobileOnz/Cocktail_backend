package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ⚠️ member_id 필드가 없다. 이것이 익명성의 근거다. 되살리지 말 것.
 * 작성자는 author_ref(= HMAC 파생 핸들)로만 식별되며, 실 회원 역추적은
 * 분리 테이블 bar_chat_identity 를 통해서만 가능하고 30일 후 파기된다.
 */
@Entity
@Table(name = "bar_chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarChatMessage {

    public static final String VISIBLE = "VISIBLE";
    public static final String HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bar_id", nullable = false)
    private Long barId;

    @Column(name = "author_ref", nullable = false, length = 32)
    private String authorRef;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "hidden_reason", length = 40)
    private String hiddenReason;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private BarChatMessage(Long barId, String authorRef, String content) {
        this.barId = barId;
        this.authorRef = authorRef;
        this.content = content;
        this.status = VISIBLE;
        this.reportCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public static BarChatMessage of(Long barId, String authorRef, String content) {
        return new BarChatMessage(barId, authorRef, content);
    }

    public void applyReport(int distinctReporters, int threshold) {
        this.reportCount = distinctReporters;
        if (distinctReporters >= threshold && VISIBLE.equals(this.status)) {
            this.status = HIDDEN;
            this.hiddenReason = "AUTO_REPORT_THRESHOLD";
        }
    }

    public boolean isVisible() {
        return VISIBLE.equals(status);
    }
}
