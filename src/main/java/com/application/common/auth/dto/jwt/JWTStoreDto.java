package com.application.common.auth.dto.jwt;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class JWTStoreDto {
    private final String refreshToken;
    private final LocalDateTime createTime;


    public JWTStoreDto(String jwtRefreshToken){
        this.refreshToken = jwtRefreshToken;
        this.createTime = LocalDateTime.now();
    }

}
