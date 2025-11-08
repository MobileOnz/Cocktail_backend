package com.application.domain.cocktail.enums;

import lombok.Getter;

@Getter
public enum TasteLevel {
    BEGINNER("입문", "가볍게 시작할 수 있는 단계"),
    NORMAL("기본", "칵테일에 대한 관심이 있는 단계"),
    INTERMEDIATE("중급", "칵테일을 자주 마시는 단게"),
    ADVANCED("고급", "칵테일에 대한 전문가");

    private final String label;
    private final String comment;

    TasteLevel(String label, String comment) {
        this.label = label;
        this.comment = comment;
    }

}
