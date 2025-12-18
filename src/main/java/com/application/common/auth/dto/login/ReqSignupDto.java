package com.application.common.auth.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "회원가입 요청 DTO")
public class ReqSignupDto {

    @NotNull
    @JsonProperty("code")
    @Schema(description = "소셜 로그인 시 받은 임시 코드 (신규 회원에게만 발급됨)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String code;

    @NotNull
    @JsonProperty("nickName")
    @Schema(description = "사용자 닉네임", example = "칵테일러버")
    private String nickName;

    @JsonProperty("deviceNumber")
    @Schema(description = "사용자 기기 번호 (사용자의 로그인/비로그인 상태를 포괄할 수 있는 디바이스 고유 ID) -> 모니터링에 사용", example = "device_unique_identifier_12345")
    private String deviceNumber;

    @NotNull
    @JsonProperty("ageTerm")
    @Schema(description = "만 14세 이상 약관 동의 여부 (필수)", example = "true")
    private Boolean ageTerm;

    @NotNull
    @JsonProperty("serviceTerm")
    @Schema(description = "서비스 이용약관 동의 여부 (필수)", example = "true")
    private Boolean serviceTerm;

    @JsonProperty("marketingTerm")
    @Schema(description = "마케팅 정보 수신 동의 여부 (선택)", example = "false", defaultValue = "false")
    private Boolean marketingTerm = false;

    @JsonProperty("adTerm")
    @Schema(description = "광고성 정보 수신 동의 여부 (선택)", example = "false", defaultValue = "false")
    private Boolean adTerm = false;
}
