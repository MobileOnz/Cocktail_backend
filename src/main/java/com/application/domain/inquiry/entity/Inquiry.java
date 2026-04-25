package com.application.domain.inquiry.entity;

import com.application.common.time.BaseTimeEntity;
import com.application.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "inquiry")
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "device_number")
    private String deviceNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "contact")
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Builder
    public Inquiry(Member member, String deviceNumber, String title, String content,
                   String contact, InquiryStatus status, String adminMemo) {
        this.member = member;
        this.deviceNumber = deviceNumber;
        this.title = title;
        this.content = content;
        this.contact = contact;
        this.status = status != null ? status : InquiryStatus.NEW;
        this.adminMemo = adminMemo;
    }

    public void markRead() {
        if (this.status == InquiryStatus.NEW) {
            this.status = InquiryStatus.READ;
        }
    }

    public void updateStatus(InquiryStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void updateAdminMemo(String memo) {
        this.adminMemo = memo;
    }
}
