package com.application.common.auth.dto.jwt;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JWTAccessBlackListDto {
    private String accessToken;
    private LocalDateTime createTime;

    public JWTAccessBlackListDto(String token){
        this.accessToken = token;
        this.createTime = LocalDateTime.now();
    }
}
