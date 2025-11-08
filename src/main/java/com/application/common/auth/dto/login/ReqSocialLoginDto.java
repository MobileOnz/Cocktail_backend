package com.application.common.auth.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReqSocialLoginDto {
    @NotNull
    @JsonProperty("provider")
    private String provider;
    @JsonProperty("code")
    private String code;
    @JsonProperty("state")
    private String state;
    @JsonProperty("accessToken")
    private String accessToken;


    @Override
    public String toString(){
        return "[DTO] Provider : " + provider +"\n[DTO] code : " + code + "\n[DTO] state : " + state +"\n[DTO] accessToken : " + accessToken;
    }
}
