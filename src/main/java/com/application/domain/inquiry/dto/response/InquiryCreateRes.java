package com.application.domain.inquiry.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "1:1 문의 등록 응답 DTO")
public class InquiryCreateRes {

    @JsonProperty("id")
    @Schema(description = "등록된 문의 ID", example = "1")
    private final Long id;

    @Builder
    public InquiryCreateRes(Long id) {
        this.id = id;
    }
}
