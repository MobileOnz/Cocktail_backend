package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.enums.AbvLevel;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 클라이언트에게 반환될 칵테일 정보 DTO (record)
 * record는 불변(Immutable)하며 Getter, Constructor 등이 자동 생성됩니다.
 */
@Schema(description = "칵테일 상세 정보 응답 DTO")
public record CocktailResponseDto(
        @Schema(description = "칵테일 ID", example = "10")
        Long id,

        @Schema(description = "칵테일 이름", example = "마티니")
        String korName,

        String engName,

        AbvLevel abvBand,
        Integer maxAlcohol,
        Integer minAlcohol,
        String originText,
        String season,
        String ingredientsText,
        String style,
        String glassType,
        String base

) {
    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드는 record에서도 유효합니다.
    public static CocktailResponseDto from(Cocktail cocktail) {
        return new CocktailResponseDto(
                cocktail.getId(),
                cocktail.getKorName(),
                cocktail.getEngName(),
                cocktail.getAbvBand(),         // Enum 필드
                cocktail.getMaxAlcohol(),
                cocktail.getMinAlcohol(),
                cocktail.getOriginText(),
                cocktail.getSeason(),
                cocktail.getIngredientsText(),
                cocktail.getStyle(),
                cocktail.getGlassType(),
                cocktail.getBase()
        );
    }
}