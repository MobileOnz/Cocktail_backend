package com.application.domain.cocktail.dto.request;

import com.application.domain.cocktail.enums.recommendation.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 클라이언트가 맞춤추천 결과를 받기 위한 조건 DTO (record)
 * record는 Lombok 없이도 모든 JSON 역직렬화를 지원합니다.
 */
@Schema(description = "칵테일 맞춤 추천 DTO")
public record CocktailRecommendationDto(

        @Schema(description = "선호하는 맛 (단일 선택)", example = "SWEET")
        Flavor flavor,

        @Schema(description = "선호하는 분위기 (단일 선택)", example = "PARTY")
        Mood mood,

        @Schema(description = "선호하는 계절", example = "SUMMER")
        Season season,

        @Schema(description = "스타일", example = "LIGHT")
        Style style,

        @Schema(description = "도수 레벨", example = "LOW")
        AbvLevel abvBand

) {
    // record는 별도의 생성자나 setter/getter 없이 이대로 바로 사용 가능합니다.
}