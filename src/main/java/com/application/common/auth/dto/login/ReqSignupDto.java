package com.application.common.auth.dto.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class ReqSignupDto {

    @NotNull
    @JsonProperty("code")
    private String code;
    @NotNull
    @JsonProperty("nickName")
    private String nickName;
    @NotNull
    @JsonProperty("ageTerm")
    private Boolean ageTerm;
    @NotNull
    @JsonProperty("serviceTerm")
    private Boolean serviceTerm;
    @JsonProperty("marketingTerm")
    private Boolean marketingTerm = false;
    @JsonProperty("adTerm")
    private Boolean adTerm = false;
}
