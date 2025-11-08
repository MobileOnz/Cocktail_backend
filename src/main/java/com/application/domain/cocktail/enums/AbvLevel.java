package com.application.domain.cocktail.enums;

import lombok.Getter;

@Getter
public enum AbvLevel {
    WEAK("약함"),
    NORMAL("보통"),
    STRONG("강함");

    private final String abvLevel;
    AbvLevel(String abvLevel) {
        this.abvLevel = abvLevel;
    }

}
