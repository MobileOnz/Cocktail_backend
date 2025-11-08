package com.application.common.auth.dto.oauth2Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private String credentialId;
    private String email;
    private String name;
    private String role;


    public UserDTO(String credentialId, String email, String name, String role){
        this.credentialId = credentialId;
        this.email = email;
        this.name = name;
        this.role = role;
    }


    public UserDTO(String credentialId, String role){
        this.credentialId = credentialId;
        this.role = role;
    }
}
