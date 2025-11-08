package com.application.domain.cocktail.enums;

import lombok.Getter;

@Getter
public enum Season {
    SPRING("봄"),
    SUMMER("여름"),
    FALL("가을"),
    WINTER("겨울");

    private final String seasonName;
    Season(String name){
        this.seasonName = name;
    }
}
