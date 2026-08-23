package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** UNIQUE(message_id, reporter_ref) 로 자작 신고폭탄을 막는다. */
@Entity
@Table(name = "bar_chat_report",
       uniqueConstraints = @UniqueConstraint(name = "uk_chat_report_once",
                                             columnNames = {"message_id", "reporter_ref"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "reporter_ref", nullable = false, length = 32)
    private String reporterRef;

    @Column(name = "reason", nullable = false, length = 16)
    private String reason;

    @Column(name = "detail", length = 300)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private BarChatReport(Long messageId, String reporterRef, String reason, String detail) {
        this.messageId = messageId;
        this.reporterRef = reporterRef;
        this.reason = reason;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }

    public static BarChatReport of(Long messageId, String reporterRef, String reason, String detail) {
        return new BarChatReport(messageId, reporterRef, reason, detail);
    }
}
