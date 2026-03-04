package com.application.domain.cocktail.enums.recommendation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 칵테일의 스타일을 정의하는 Enum.
 * 클라이언트가 선택하는 5가지 스타일 필터링 조건으로 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum Style {
    LIGHT("라이트"),
    STANDARD("스탠다드"),
    SPECIAL("스페셜"),
    STRONG("스트롱"),
    CLASSIC("클래식");

    private final String description;
}