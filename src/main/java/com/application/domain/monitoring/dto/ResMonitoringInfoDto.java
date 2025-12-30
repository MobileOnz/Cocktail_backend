package com.application.domain.monitoring.dto;

import com.application.domain.member.enums.AgeRange;
import com.application.domain.member.enums.Gender;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "모니터링 정보 조회 응답 DTO")
public class ResMonitoringInfoDto {

    @JsonProperty("deviceNumber")
    @Schema(description = "기기 고유 번호", example = "device_unique_identifier_12345")
    private String deviceNumber;

    @JsonProperty("isMember")
    @Schema(description = "회원 여부", example = "true")
    private Boolean isMember;

    @JsonProperty("memberId")
    @Schema(description = "회원 ID (회원인 경우에만)", example = "123")
    private Long memberId;

    @JsonProperty("age")
    @Schema(description = "나이 (회원인 경우에만)", example = "25")
    private Integer age;

    @JsonProperty("ageRange")
    @Schema(description = "연령대 (회원인 경우에만)", example = "TWENTIES")
    private AgeRange ageRange;

    @JsonProperty("gender")
    @Schema(description = "성별 (회원인 경우에만)", example = "MALE")
    private Gender gender;

    @JsonProperty("totalCount")
    @Schema(description = "전체 접근 횟수 (회원: 모든 기기 합산, 비회원: 기기별)", example = "50")
    private Long totalCount;

    @Builder
    public ResMonitoringInfoDto(String deviceNumber, Boolean isMember, Long memberId,
                                 Integer age, AgeRange ageRange, Gender gender, Long totalCount) {
        this.deviceNumber = deviceNumber;
        this.isMember = isMember;
        this.memberId = memberId;
        this.age = age;
        this.ageRange = ageRange;
        this.gender = gender;
        this.totalCount = totalCount;
    }
}
