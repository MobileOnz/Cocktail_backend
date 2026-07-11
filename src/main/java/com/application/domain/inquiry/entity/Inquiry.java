package com.application.domain.inquiry.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 문의(1:1). 감사 F-05: 대시보드가 이 테이블을 네이티브 SQL로 조회하는데 엔티티/테이블이
 * 없어 새 DB에서 500이 났다. V2에서 테이블을 만들고 여기서 엔티티로 정식화한다.
 */
@Entity
@Table(name = "inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 20)
    private String status;   // NEW | READ | REPLIED

    @Column(columnDefinition = "text")
    private String reply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Inquiry(Long memberId, String email, String title, String content, String status) {
        this.memberId = memberId;
        this.email = email;
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void reply(String reply) {
        this.reply = reply;
        this.status = "REPLIED";
        this.repliedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "NEW";
        }
    }
}
