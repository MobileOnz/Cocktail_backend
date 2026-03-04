package com.application.domain.monitoring.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "비회원 온보딩 정보 저장 요청 DTO")
public class SaveOnboardingReq {

    @NotBlank(message = "기기 번호는 필수입니다.")
    @JsonProperty("deviceNumber")
    @Schema(description = "기기 고유 번호", example = "device_unique_identifier_12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceNumber;

    @Schema(
            description = "성별 정보",
            example = "male",
            allowableValues = {"male", "female", "none"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "성별 정보는 필수입니다.")
    @JsonProperty("gender")
    private String gender;

    @Schema(
            description = """
                    연령대 정보
                    - under_19: 19세 이하
                    - 20_24: 20-24세
                    - 25_29: 25-29세
                    - 30_34: 30-34세
                    - 35_39: 35-39세
                    - 50_over: 50세 이상
                    """,
            example = "20_24",
            allowableValues = {"under_19", "20_24", "25_29", "30_34", "35_39", "50_over"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "연령대 정보는 필수입니다.")
    @JsonProperty("ageRange")
    private String ageRange;
}
