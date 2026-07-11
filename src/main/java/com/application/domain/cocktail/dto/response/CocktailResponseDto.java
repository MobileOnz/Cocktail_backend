package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailFlavor;
import com.application.domain.cocktail.entity.CocktailMood;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Schema(description = "칵테일 상세 정보 응답 DTO")
public record CocktailResponseDto(
        @Schema(description = "칵테일 ID", example = "10")
        Long id,

        @Schema(description = "한글 이름", example = "마티니")
        String korName,

        @Schema(description = "영어 이름", example = "Martini")
        String engName,

        @Schema(description = "도수 레벨", example = "STRONG")
        AbvLevel abvBand,

        @Schema(description = "최대 알코올 도수", example = "40")
        Integer maxAlcohol,

        @Schema(description = "최소 알코올 도수", example = "30")
        Integer minAlcohol,

        @Schema(description = "칵테일 유래 설명")
        String originText,

        @Schema(description = "추천 계절", example = "여름")
        String season,

        @Schema(description = "재료 목록", example = "[\"진 60ml\", \"드라이 베르무트 10ml\", \"올리브\"]")
        List<String> ingredients,

        @Schema(description = "스타일", example = "클래식")
        String style,

        @Schema(description = "글라스 타입", example = "칵테일 글라스")
        String glassType,

        @Schema(description = "글라스 이미지 URL")
        String glassImageUrl,

        @Schema(description = "베이스 술", example = "진")
        String base,

        @Schema(description = "칵테일 이미지 URL")
        String imageUrl,

        @Schema(description = "맛 태그 목록", example = "[\"상큼한\", \"달콤한\"]")
        List<String> flavors,

        @Schema(description = "분위기 태그 목록", example = "[\"파티\", \"데이트\"]")
        List<String> moods,

        @Schema(description = "즐겨찾기 여부", example = "true")
        boolean isBookmarked,

        @Schema(description = "현재 나의 반응 상태 (RECOMMEND, HARD, null)", example = "RECOMMEND")
        ReactionType myReaction,

        @Schema(description = "추천해요 총 개수", example = "15")
        Integer recommendCount,

        @Schema(description = "어려워요 총 개수", example = "3")
        Integer hardCount,

        @Schema(description = "생성 날짜", example = "2024-01-01T12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 날짜", example = "2024-01-02T12:00:00")
        LocalDateTime updatedAt
) {

    public static CocktailResponseDto from(Cocktail cocktail) {
        return from(cocktail, null, null, null, null);
    }

    public static CocktailResponseDto from(Cocktail cocktail, Long userId) {
        return from(cocktail, userId, null, null, null);
    }

    public static CocktailResponseDto from(Cocktail cocktail, Long userId, ReactionType myReaction, Integer recommendCount, Integer hardCount) {
        return new CocktailResponseDto(
                cocktail.getId(),
                cocktail.getKorName(),
                cocktail.getEngName(),
                cocktail.getAbvBand(),
                cocktail.getMaxAlcohol(),
                cocktail.getMinAlcohol(),
                cocktail.getOriginText(),
                cocktail.getSeason(),
                parseIngredients(cocktail.getIngredientsText()),
                cocktail.getStyle(),
                cocktail.getGlassType(),
                cocktail.getGlassImageUrl(),
                cocktail.getBase(),
                cocktail.getImageUrl(),
                parseFlavors(cocktail),
                parseMoods(cocktail),
                cocktail.isBookmarkedBy(userId),
                myReaction,
                clampNonNegative(recommendCount != null ? recommendCount : cocktail.getRecommendCount()),
                hardCount != null ? hardCount : cocktail.getHardCount(),
                cocktail.getCreatedAt(),
                cocktail.getUpdatedAt()
        );
    }

    public static CocktailResponseDto from(Cocktail cocktail, ReactionType myReaction, Integer recommendCount, Integer hardCount, boolean isBookmarked) {
        return new CocktailResponseDto(
                cocktail.getId(),
                cocktail.getKorName(),
                cocktail.getEngName(),
                cocktail.getAbvBand(),
                cocktail.getMaxAlcohol(),
                cocktail.getMinAlcohol(),
                cocktail.getOriginText(),
                cocktail.getSeason(),
                parseIngredients(cocktail.getIngredientsText()),
                cocktail.getStyle(),
                cocktail.getGlassType(),
                cocktail.getGlassImageUrl(),
                cocktail.getBase(),
                cocktail.getImageUrl(),
                parseFlavors(cocktail),
                parseMoods(cocktail),
                isBookmarked,
                myReaction,
                clampNonNegative(recommendCount != null ? recommendCount : cocktail.getRecommendCount()),
                hardCount != null ? hardCount : cocktail.getHardCount(),
                cocktail.getCreatedAt(),
                cocktail.getUpdatedAt()
        );
    }

    public static CocktailResponseDto from(Cocktail cocktail, boolean isBookmarked) {
        return new CocktailResponseDto(
                cocktail.getId(),
                cocktail.getKorName(),
                cocktail.getEngName(),
                cocktail.getAbvBand(),
                cocktail.getMaxAlcohol(),
                cocktail.getMinAlcohol(),
                cocktail.getOriginText(),
                cocktail.getSeason(),
                parseIngredients(cocktail.getIngredientsText()),
                cocktail.getStyle(),
                cocktail.getGlassType(),
                cocktail.getGlassImageUrl(),
                cocktail.getBase(),
                cocktail.getImageUrl(),
                parseFlavors(cocktail),
                parseMoods(cocktail),
                isBookmarked,
                null,
                clampNonNegative(cocktail.getRecommendCount()),
                cocktail.getHardCount(),
                cocktail.getCreatedAt(),
                cocktail.getUpdatedAt()
        );
    }

    private static List<String> parseIngredients(String ingredientsText) {
        if (ingredientsText == null || ingredientsText.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(ingredientsText.split(","))
                .map(String::trim)
                .toList();
    }

    private static List<String> parseFlavors(Cocktail cocktail) {
        return cocktail.getFlavors().stream()
                .map(CocktailFlavor::getFlavorName)
                .toList();
    }

    private static List<String> parseMoods(Cocktail cocktail) {
        return cocktail.getMoods().stream()
                .map(CocktailMood::getMoodName)
                .toList();
    }

    /** 추천수는 음수로 노출되면 안 된다(QA P3-7). null 은 0, 음수는 0 으로 하한. */
    private static Integer clampNonNegative(Integer count) {
        return count == null ? 0 : Math.max(0, count);
    }
}