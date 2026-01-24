package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailFlavor;
import com.application.domain.cocktail.entity.CocktailMood;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

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

        // 프론트 요구사항에 맞게 배열로 응답하도록 수정
//        String ingredientsText,
        List<String> ingredients,

        String style,

        // glassType이 여러 개가 있는 칵테일이 있어 수정 -> 다시 원복
        String glassType,
//        @Schema(description = "사용되는 글라스 타입 목록", example = "[\"칵테일 글라스\", \"마티니 글라스\"]")
//        List<String> glassTypes,

        String glassImageUrl,
        String base,

        String imageUrl,

        // 맛 태그 리스트
        @Schema(description = "맛 태그 목록", example = "[\"상큼한\", \"달콤한\"]")
        List<String> flavors,

        // 분위기 태그 리스트
        @Schema(description = "분위기 태그 목록", example = "[\"파티\", \"데이트\"]")
        List<String> moods,

        @Schema(description = "즐겨찾기 여부", example = "true")
        boolean isBookmarked,

        // 반응 정보 추가
        @Schema(description = "현재 나의 반응 상태 (null이면 아무것도 안 누름)", example = "RECOMMEND")
        ReactionType myReaction,

        @Schema(description = "추천해요 총 개수", example = "15")
        Integer recommendCount,

        @Schema(description = "어려워요 총 개수", example = "3")
        Integer hardCount

) {
    public static CocktailResponseDto from(Cocktail cocktail) {
        return from(cocktail, null, null, null, null);
    }

    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드는 record에서도 유효합니다.
    public static CocktailResponseDto from(Cocktail cocktail, Long userId) {
        return from(cocktail, userId, null, null, null);
    }

    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드 (반응 정보 포함)
    public static CocktailResponseDto from(Cocktail cocktail, Long userId, ReactionType myReaction, Integer recommendCount, Integer hardCount) {

//        String rawGlassType = cocktail.getGlassType();
//        // glassType 문자열을 '/' 기준으로 분리하여 List로 변환
//        List<String> splitGlassTypes = (rawGlassType != null && !rawGlassType.isEmpty())
//                ? Arrays.stream(rawGlassType.split("/"))
//                .map(String::trim) // 공백 제거
//                .toList()
//                : List.of(); // 값이 없으면 빈 리스트 반환

        // 재료 리스트로 전달하도록 수정
        String rawIngredientsText = cocktail.getIngredientsText();
        List<String> ingredients = (rawIngredientsText != null && !rawIngredientsText.isEmpty())
                ? Arrays.stream(rawIngredientsText.split(","))
                .map(String::trim) // 공백 제거
                .toList()
                : List.of(); // 값이 없으면 빈 리스트 반환

        return new CocktailResponseDto(
                cocktail.getId(),
                cocktail.getKorName(),
                cocktail.getEngName(),
                cocktail.getAbvBand(),         // Enum 필드
                cocktail.getMaxAlcohol(),
                cocktail.getMinAlcohol(),
                cocktail.getOriginText(),
                cocktail.getSeason(),

                // 재료 list로 변경에 따른 수정
                ingredients,
//                cocktail.getIngredientsText(),

                cocktail.getStyle(),

                cocktail.getGlassType(), // 잔 list로 변경에 따른 수정 -> 원복
//                splitGlassTypes,

                cocktail.getGlassImageUrl(),
                cocktail.getBase(),

                cocktail.getImageUrl(),

                // [매핑 로직] Entity List -> String List 변환
                // application.properties의 batch_fetch_size 덕분에 여기서 성능 저하 없이 조회됨
                cocktail.getFlavors().stream()
                        .map(CocktailFlavor::getFlavorName)
                        .toList(),

                cocktail.getMoods().stream()
                        .map(CocktailMood::getMoodName)
                        .toList(),

                cocktail.isBookmarkedBy(userId),

                // 반응 정보
                myReaction,
                recommendCount != null ? recommendCount : cocktail.getRecommendCount(),
                hardCount != null ? hardCount : cocktail.getHardCount()
        );
    }

    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드 (북마크 여부만 지정)
    public static CocktailResponseDto from(Cocktail cocktail, boolean isBookmarked) {

//        String rawGlassType = cocktail.getGlassType();
//        // glassType 문자열을 '/' 기준으로 분리하여 List로 변환
//        List<String> splitGlassTypes = (rawGlassType != null && !rawGlassType.isEmpty())
//                ? Arrays.stream(rawGlassType.split("/"))
//                .map(String::trim) // 공백 제거
//                .toList()
//                : List.of(); // 값이 없으면 빈 리스트 반환

        // 재료 리스트로 전달하도록 수정
        String rawIngredientsText = cocktail.getIngredientsText();
        List<String> ingredients = (rawIngredientsText != null && !rawIngredientsText.isEmpty())
                ? Arrays.stream(rawIngredientsText.split(","))
                .map(String::trim) // 공백 제거
                .toList()
                : List.of(); // 값이 없으면 빈 리스트 반환

        return new CocktailResponseDto(
                cocktail.getId(),
                cocktail.getKorName(),
                cocktail.getEngName(),
                cocktail.getAbvBand(),         // Enum 필드
                cocktail.getMaxAlcohol(),
                cocktail.getMinAlcohol(),
                cocktail.getOriginText(),
                cocktail.getSeason(),

                // 재료 list로 변경에 따른 수정
                ingredients,
//                cocktail.getIngredientsText(),

                cocktail.getStyle(),

                cocktail.getGlassType(), // 잔 list로 변경에 따른 수정 -> 원복
//                splitGlassTypes,

                cocktail.getGlassImageUrl(),
                cocktail.getBase(),

                cocktail.getImageUrl(),

                // [매핑 로직] Entity List -> String List 변환
                // application.properties의 batch_fetch_size 덕분에 여기서 성능 저하 없이 조회됨
                cocktail.getFlavors().stream()
                        .map(CocktailFlavor::getFlavorName)
                        .toList(),

                cocktail.getMoods().stream()
                        .map(CocktailMood::getMoodName)
                        .toList(),

                isBookmarked,

                // 반응 정보 (기본값)
                null,
                cocktail.getRecommendCount(),
                cocktail.getHardCount()
        );
    }
}