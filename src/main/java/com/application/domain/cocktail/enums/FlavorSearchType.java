package com.application.domain.cocktail.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 칵테일 검색 필터용 맛 카테고리 (9가지)
 */
@Getter
@RequiredArgsConstructor
public enum FlavorSearchType {
    FRUIT("과일"),
    BITTER("쌉쌀함"),
    SWEET("달콤함"),
    CREAMY("부드러움"),
    COMPLEX("복합적인 맛"),
    HERBAL_SPICE("허브 & 스파이스"),
    LIGHT_REFRESHING("라이트 & 청량함"),
    STRONG_UNIQUE("개성 강한 맛"),
    ETC_SPECIAL("기타 & 특별한 맛");

    private final String description;
}