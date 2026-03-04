package com.application.common.auth.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "회원가입 대기 응답 DTO (신규 회원 판별 시)")
public class ResSignupDto implements ResSocialLoginDto{
    @Schema(description = "회원가입을 위한 임시 코드 (약관 동의 시 사용)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String code;

    @Schema(description = "응답 타입 (signup: 회원가입 필요)", example = "signup")
    private String type;

    @Builder
    public ResSignupDto(String code){
        this.code = code;
        this.type = "signup";
    }
}
