package com.application.domain.cocktail.enums.recommendation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 칵테일이 어울리는 분위기나 상황을 정의하는 Enum.
 * 클라이언트가 선택하는 6가지 분위기 필터링 조건으로 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum Mood {
    MEAL_TIME("식전 식후"),
    ROMANTIC("데이트 로맨틱"),
    PARTY("파티 여럿이"),
    CASUAL("집에서 간단히"),
    MODERN("세련된 모던"),
    CLASSIC("클래식 전통");

    private final String description;
}