package com.application.domain.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "페이지 접근 추적 응답 DTO")
public class ResTrackingDto {

    @JsonProperty("deviceNumber")
    @Schema(description = "기기 고유 번호", example = "device_unique_identifier_12345")
    private String deviceNumber;

    @JsonProperty("count")
    @Schema(description = "현재 접근 횟수 (비회원: 기기별 count, 회원: 전체 count)", example = "1")
    private Long count;

    @JsonProperty("isFirstAccess")
    @Schema(description = "최초 접근 여부", example = "true")
    private Boolean isFirstAccess;

    @JsonProperty("createdAt")
    @Schema(description = "최초 접근 시간 (DB 레코드 생성 시간)", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @JsonProperty("isMember")
    @Schema(description = "회원 여부 (true: 회원, false: 비회원)", example = "false")
    private Boolean isMember;

    @JsonProperty("memberId")
    @Schema(description = "회원 ID (회원인 경우에만 존재)", example = "123")
    private Long memberId;

    @Builder
    public ResTrackingDto(String deviceNumber, Long count, Boolean isFirstAccess, LocalDateTime createdAt, Boolean isMember, Long memberId) {
        this.deviceNumber = deviceNumber;
        this.count = count;
        this.isFirstAccess = isFirstAccess;
        this.createdAt = createdAt;
        this.isMember = isMember;
        this.memberId = memberId;
    }
}