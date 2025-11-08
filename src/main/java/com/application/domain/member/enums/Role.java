package com.application.domain.member.enums;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum Role {
    ADMIN("ROLE_ADMIN"),
    USER ("ROLE_USER"),
    OTHER("ROLE_OTHER");

    private String english;


    Role(String eng){
        this.english = eng;
    }


    public static Optional<Role> fromROLE(String role){
        for (Role value : Role.values()) {
            if(value.getEnglish().equalsIgnoreCase(role))
                return Optional.of(value);
        }
        return Optional.empty();
    }
}
