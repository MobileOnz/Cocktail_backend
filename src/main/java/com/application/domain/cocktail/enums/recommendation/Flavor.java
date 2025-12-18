package com.application.domain.cocktail.enums.recommendation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 칵테일의 주요 맛 카테고리를 정의하는 Enum.
 * 클라이언트가 선택하는 상위 7가지 맛 필터링 조건으로 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum Flavor {
    SWEET("달콤한 맛"),
    SPARKLING("청량 스파클링"),
    CITRUS("상큼 시트러스"),
    TROPICAL("과일향 트로피컬"),
    BITTER("쌉싸름 비터"),
    SPICY("스파이시 따뜻한"),
    HERBAL("허브 프레시");

    private final String description;
}
