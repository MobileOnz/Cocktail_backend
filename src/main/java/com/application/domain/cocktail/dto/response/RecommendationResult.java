package com.application.domain.cocktail.dto.response;

/**
 * 개인화 추천 결과 (T-09). heroReason 은 실제 근거 문장(룰에서 생성). ruleTag 는 적용된 룰 식별자.
 */
public record RecommendationResult(
        Long cocktailId,
        String name,
        String imageUrl,
        String heroReason,
        String ruleTag
) {}
