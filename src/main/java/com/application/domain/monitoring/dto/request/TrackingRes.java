package com.application.domain.monitoring.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "페이지 접근 추적 응답 DTO")
public class TrackingRes {

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

    // F-14: /track 은 인증 없이 호출되므로(비로그인 트래킹 목적), 다른 회원의 내부 memberId를
    // 임의의 deviceNumber로 알아낼 수 있는 필드는 응답에서 제외한다. isMember 플래그만 남긴다.
    @Builder
    public TrackingRes(String deviceNumber, Long count, Boolean isFirstAccess, LocalDateTime createdAt, Boolean isMember) {
        this.deviceNumber = deviceNumber;
        this.count = count;
        this.isFirstAccess = isFirstAccess;
        this.createdAt = createdAt;
        this.isMember = isMember;
    }
}