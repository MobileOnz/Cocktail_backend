package com.application.domain.main.dto;

import com.application.domain.cocktail.dto.response.RecommendationResult;

/**
 * 메인 히어로(오늘의 추천 1개) (T-09). 필드 camelCase.
 */
public record HeroDto(
        Long cocktailId,
        String name,
        String imageUrl,
        String heroReason
) {
    public static HeroDto from(RecommendationResult r) {
        if (r == null) return null;
        return new HeroDto(r.cocktailId(), r.name(), r.imageUrl(), r.heroReason());
    }
}
