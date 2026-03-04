package com.application.common.auth.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "소셜 로그인 요청 DTO - 모바일 앱 방식과 웹 OAuth 방식 모두 지원")
public class ReqSocialLoginDto {
    @NotNull
    @JsonProperty("provider")
    @Schema(description = "소셜 로그인 제공자",
            example = "naver",
            allowableValues = {"kakao", "naver", "apple", "google"})
    private String provider;

    @JsonProperty("code")
    @Schema(description = "[웹 OAuth 방식] 소셜 플랫폼에서 발급받은 인증 코드. " +
            "웹 OAuth 리다이렉트 콜백으로 받은 authorization code를 전달합니다. " +
            "모바일 앱 방식에서는 사용하지 않습니다.",
            example = "authorization_code_from_provider")
    private String code;

    @JsonProperty("state")
    @Schema(description = "[웹 OAuth 방식 - 네이버만 해당] CSRF 방지를 위한 state 값. " +
            "네이버 웹 OAuth 사용 시에만 필요하며, 모바일 앱 방식에서는 사용하지 않습니다.",
            example = "random_state_string")
    private String state;

    @JsonProperty("accessToken")
    @Schema(description = "[모바일 앱 방식] 소셜 플랫폼 SDK에서 직접 발급받은 액세스 토큰. " +
            "React Native, Flutter 등 모바일 앱에서 네이버/카카오/구글 SDK로 받은 토큰을 전달합니다. " +
            "code 또는 accessToken 중 하나는 필수입니다.",
            example = "AAAANvMf...mobile_app_access_token")
    private String accessToken;

    @JsonProperty("deviceNumber")
    @Schema(description = "사용자 기기 고유 번호 (로그인 시 기기-회원 매핑에 사용)",
            example = "device_unique_identifier_12345")
    private String deviceNumber;


    @Override
    public String toString(){
        return "[DTO] Provider : " + provider +"\n[DTO] code : " + code + "\n[DTO] state : " + state +"\n[DTO] accessToken : " + accessToken + "\n[DTO] deviceNumber : " + deviceNumber;
    }
}
