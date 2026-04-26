package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailFlavor;
import com.application.domain.cocktail.entity.CocktailMood;
import com.application.domain.cocktail.enums.AbvLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

/**
 * 로그인한 사용자의 칵테일 상세 조회 시 북마크 여부를 포함한 DTO
 */
@Schema(description = "칵테일 상세 정보 응답 DTO (북마크 여부 포함)")
public record CocktailDetailResponseDto(
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
        List<String> ingredients,
        String style,
        String glassType,
        String glassImageUrl,
        String glassImageUrlThumb,
        String glassImageUrlDetail,
        String base,
        String imageUrl,
        String imageUrlThumb,
        String imageUrlDetail,

        @Schema(description = "맛 태그 목록", example = "[\"상큼한\", \"달콤한\"]")
        List<String> flavors,

        @Schema(description = "분위기 태그 목록", example = "[\"파티\", \"데이트\"]")
        List<String> moods,

        @Schema(description = "북마크 여부 (로그인 사용자만, 비로그인은 null)", example = "true")
        Boolean isBookmarked
) {
    public static CocktailDetailResponseDto from(Cocktail cocktail, Boolean isBookmarked) {
        String rawIngredientsText = cocktail.getIngredientsText();
        List<String> ingredients = (rawIngredientsText != null && !rawIngredientsText.isEmpty())
                ? Arrays.stream(rawIngredientsText.split(","))
                .map(String::trim)
                .toList()
                : List.of();

        return new CocktailDetailResponseDto(
                cocktail.getId(),
                cocktail.getKorName(),
                cocktail.getEngName(),
                cocktail.getAbvBand(),
                cocktail.getMaxAlcohol(),
                cocktail.getMinAlcohol(),
                cocktail.getOriginText(),
                cocktail.getSeason(),
                ingredients,
                cocktail.getStyle(),
                cocktail.getGlassType(),
                cocktail.getGlassImageUrl(),
                cocktail.getGlassImageUrlThumb(),
                cocktail.getGlassImageUrlDetail(),
                cocktail.getBase(),
                cocktail.getImageUrl(),
                cocktail.getImageUrlThumb(),
                cocktail.getImageUrlDetail(),
                cocktail.getFlavors().stream()
                        .map(CocktailFlavor::getFlavorName)
                        .toList(),
                cocktail.getMoods().stream()
                        .map(CocktailMood::getMoodName)
                        .toList(),
                isBookmarked
        );
    }
}
