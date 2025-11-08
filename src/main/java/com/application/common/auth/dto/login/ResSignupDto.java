package com.application.common.auth.dto.login;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ResSignupDto implements ResSocialLoginDto{
    private String code;
    private String type;

    @Builder
    public ResSignupDto(String code){
        this.code = code;
        this.type = "signup";
    }
}
