package com.application.domain.cocktail.enums.recommendation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 칵테일이 어울리는 계절을 정의하는 Enum.
 * 클라이언트가 선택하는 5가지 계절 필터링 조건으로 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum Season {
    SPRING("봄"),
    SUMMER("여름"),
    AUTUMN("가을"),
    WINTER("겨울"),
    ALL("사계절");

    private final String description;
}