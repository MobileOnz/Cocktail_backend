package com.application.domain.inquiry.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "1:1 문의 등록 요청 DTO")
public class InquiryCreateReq {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title max length is 200")
    @JsonProperty("title")
    @Schema(description = "문의 제목", example = "앱이 느려요", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "content is required")
    @JsonProperty("content")
    @Schema(description = "문의 내용", example = "iPhone 15 Pro에서 앱 로딩이 너무 느립니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Size(max = 255)
    @JsonProperty("contact")
    @Schema(description = "답변 받을 연락처 / 이메일 (선택)", example = "user@example.com")
    private String contact;

    @Size(max = 255)
    @JsonProperty("device_number")
    @Schema(description = "기기 고유 번호 (비로그인 사용자는 필수)", example = "device_unique_identifier_12345")
    private String deviceNumber;
}
