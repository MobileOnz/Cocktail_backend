package com.application.domain.member.enums;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum Gender {
    MALE("male"),
    FEMALE("female"),
    NONE("none");

    private String english;

    Gender(String eng){
        this.english = eng;
    }



    public static Optional<Gender> fromString(String gender){
        for (Gender value : Gender.values()) {
            if(value.getEnglish().equalsIgnoreCase(gender))
                return Optional.of(value);
        }
        return Optional.empty();
    }

}
