package com.application.domain.monitoring.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "페이지 접근 추적 요청 DTO")
public class TrackingReq {

    @NotNull
    @JsonProperty("deviceNumber")
    @Schema(description = "사용자 기기 고유 번호", example = "device_unique_identifier_12345")
    private String deviceNumber;

    @NotNull
    @JsonProperty("count")
    @Schema(description = "현재 접근 총 횟수 (프론트에서 관리)", example = "5")
    private Long count;
}