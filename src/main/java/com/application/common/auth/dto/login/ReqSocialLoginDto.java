package com.application.common.auth.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "소셜 로그인 요청 DTO")
public class ReqSocialLoginDto {
    @NotNull
    @JsonProperty("provider")
    @Schema(description = "소셜 로그인 제공자 (kakao, naver, apple, google)", example = "kakao")
    private String provider;

    @JsonProperty("code")
    @Schema(description = "소셜 플랫폼에서 발급받은 인증 코드 (code 또는 accessToken 중 하나 필수)", example = "authorization_code_from_provider")
    private String code;

    @JsonProperty("state")
    @Schema(description = "CSRF 방지를 위한 state 값 (네이버 로그인 시 필요)", example = "random_state_string")
    private String state;

    @JsonProperty("accessToken")
    @Schema(description = "소셜 플랫폼에서 발급받은 액세스 토큰 (code 또는 accessToken 중 하나 필수)", example = "access_token_from_provider")
    private String accessToken;

    @JsonProperty("deviceNumber")
    @Schema(description = "사용자 기기 번호 (사용자의 로그인/비로그인 상태를 포괄할 수 있는 디바이스 고유 ID) -> 모니터링에 사용", example = "device_unique_identifier_12345")
    private String deviceNumber;


    @Override
    public String toString(){
        return "[DTO] Provider : " + provider +"\n[DTO] code : " + code + "\n[DTO] state : " + state +"\n[DTO] accessToken : " + accessToken + "\n[DTO] deviceNumber : " + deviceNumber;
    }
}
