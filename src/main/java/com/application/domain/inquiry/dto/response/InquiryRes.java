package com.application.domain.inquiry.dto.response;

import com.application.domain.inquiry.entity.Inquiry;
import com.application.domain.inquiry.entity.InquiryStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "1:1 문의 응답 DTO")
public class InquiryRes {

    @JsonProperty("id")
    private final Long id;

    @JsonProperty("member_id")
    private final Long memberId;

    @JsonProperty("device_number")
    private final String deviceNumber;

    @JsonProperty("title")
    private final String title;

    @JsonProperty("content")
    private final String content;

    @JsonProperty("contact")
    private final String contact;

    @JsonProperty("status")
    private final InquiryStatus status;

    @JsonProperty("admin_memo")
    private final String adminMemo;

    @JsonProperty("created_at")
    private final LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private final LocalDateTime updatedAt;

    @Builder
    public InquiryRes(Long id, Long memberId, String deviceNumber, String title,
                      String content, String contact, InquiryStatus status,
                      String adminMemo, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.deviceNumber = deviceNumber;
        this.title = title;
        this.content = content;
        this.contact = contact;
        this.status = status;
        this.adminMemo = adminMemo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InquiryRes fromEntity(Inquiry inquiry) {
        return InquiryRes.builder()
                .id(inquiry.getId())
                .memberId(inquiry.getMember() != null ? inquiry.getMember().getId() : null)
                .deviceNumber(inquiry.getDeviceNumber())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .contact(inquiry.getContact())
                .status(inquiry.getStatus())
                .adminMemo(inquiry.getAdminMemo())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
