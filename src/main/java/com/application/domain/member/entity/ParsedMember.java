package com.application.domain.member.entity;

import com.application.domain.member.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
public class ParsedMember {

    private String credentialId;
    private String name;
    private String email;
    private Role role;

    public ParsedMember(){}

    @Builder
    public ParsedMember(String credentialId,
                        String name, String email,
                        Role role){
        this.credentialId = credentialId;
        this.name = name;
        this.email = email;
        this.role = role;
    }
}
