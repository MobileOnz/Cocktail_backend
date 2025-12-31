package com.application.domain.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "온보딩 상태 응답 DTO")
public class ResOnboardingStatusDto {

    @JsonProperty("onboardingCompleted")
    @Schema(description = "온보딩 완료 여부", example = "true")
    private Boolean onboardingCompleted;

    @JsonProperty("requiresOnboarding")
    @Schema(description = "온보딩이 필요한지 여부 (onboardingCompleted의 반대)", example = "false")
    private Boolean requiresOnboarding;

    @JsonProperty("isMember")
    @Schema(description = "회원 여부", example = "true")
    private Boolean isMember;

    @JsonProperty("deviceNumber")
    @Schema(description = "기기 고유 번호", example = "device_unique_identifier_12345")
    private String deviceNumber;
}
