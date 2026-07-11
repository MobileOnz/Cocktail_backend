package com.application.domain.cocktail.dto.response;

/**
 * 칵테일에 연결된 가이드 카드 (T-09). 필드 camelCase.
 */
public record CocktailGuideDto(
        Integer part,
        String title,
        String imageUrl
) {
    public static CocktailGuideDto from(Object[] row) {
        return new CocktailGuideDto(
                row[0] == null ? null : ((Number) row[0]).intValue(),
                row[1] == null ? null : row[1].toString(),
                row[2] == null ? null : row[2].toString()
        );
    }
}
