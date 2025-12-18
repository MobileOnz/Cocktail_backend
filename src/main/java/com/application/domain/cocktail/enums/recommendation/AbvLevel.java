package com.application.domain.cocktail.enums.recommendation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 칵테일의 도수 레벨을 정의하는 Enum.
 * 클라이언트가 선택하는 3가지 도수 레벨 필터링 조건으로 사용됩니다.
 * (약함, 보통, 강함)
 */
@Getter
@RequiredArgsConstructor
public enum AbvLevel {
    LOW("약함"),
    MEDIUM("보통"),
    HIGH("강함");

    private final String description;
}