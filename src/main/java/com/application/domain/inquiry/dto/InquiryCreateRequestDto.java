package com.application.domain.inquiry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 앱이 보내는 1:1 문의. 필드 구성은 앱의 InquiryFormScreen 이 실제로 보내는 것을 그대로 받는다.
 *
 * <p>{@code contact} 와 {@code device_number} 는 선택이다 — 로그인 사용자는 contact 만,
 * 비로그인 사용자는 둘 다 보내거나 device_number 만 보낼 수 있다.</p>
 */
@Schema(description = "1:1 문의 접수 요청")
public record InquiryCreateRequestDto(

        @Schema(description = "문의 제목", example = "로그인이 안돼요")
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
        String title,

        @Schema(description = "문의 내용", example = "카카오 로그인을 누르면 앱이 꺼집니다.")
        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 5000, message = "내용은 5000자를 넘을 수 없습니다.")
        String content,

        @Schema(description = "회신받을 연락처(이메일 등). 선택.", example = "me@example.com")
        @Size(max = 120, message = "연락처는 120자를 넘을 수 없습니다.")
        String contact,

        @Schema(description = "비로그인 문의의 기기 식별자. 선택.")
        @JsonProperty("device_number")
        @Size(max = 100)
        String deviceNumber
) {}
