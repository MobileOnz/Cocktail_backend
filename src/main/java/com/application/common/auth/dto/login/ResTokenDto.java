package com.application.common.auth.dto.login;

import lombok.Getter;

@Getter
public class ResTokenDto implements ResSocialLoginDto{
    private String accessToken;
    private String refreshToken;
    private String type;

    public ResTokenDto(String accessToken , String refreshToken){
        this.accessToken = "Bearer " + accessToken;
        this.refreshToken = refreshToken;
        this.type = "token";
    }
}
