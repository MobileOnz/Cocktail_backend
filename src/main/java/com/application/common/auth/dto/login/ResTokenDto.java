package com.application.common.auth.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "JWT 토큰 응답 DTO (기존 회원 로그인 또는 회원가입 완료 시)")
public class ResTokenDto implements ResSocialLoginDto{
    @Schema(description = "JWT Access Token (Bearer 접두사 포함)", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "응답 타입 (token: 로그인 성공)", example = "token")
    private String type;

    public ResTokenDto(String accessToken , String refreshToken){
        this.accessToken = "Bearer " + accessToken;
        this.refreshToken = refreshToken;
        this.type = "token";
    }
}
