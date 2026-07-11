package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.entity.CocktailStep;

import java.util.List;

/**
 * 제조 단계 응답 (T-07). 계약 §4.2 CocktailStep 형태 — 필드는 camelCase.
 */
public record CocktailStepDto(
        Integer stepOrder,
        String instruction,
        String imageUrl,
        Integer durationSec,
        String tip,
        List<Long> toolIds
) {
    public static CocktailStepDto from(CocktailStep s, List<Long> toolIds) {
        return new CocktailStepDto(
                s.getStepOrder(),
                s.getInstruction(),
                s.getImageUrl(),
                s.getDurationSec(),
                s.getTip(),
                toolIds
        );
    }
}
