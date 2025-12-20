package com.application.domain.cocktail.dto.request;

import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.FlavorSearchType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 클라이언트가 검색/필터링을 위해 전달하는 조건 DTO (record)
 * record는 Lombok 없이도 모든 JSON 역직렬화를 지원합니다.
 */
@Schema(description = "칵테일 검색 및 필터링 조건")
public record CocktailSearchConditionDto(
        @Schema(description = "칵테일 이름 검색어 (부분 일치)", example = "마티니")
        String korName,

        @Schema(description = "칵테일 이름 검색어 (부분 일치)", example = "Martini")
        String engName,

        @Schema(description = "도수 레벨", example = "STRONG")
        AbvLevel abvBand,

        @Schema(description = "스타일", example = "스트롱")
        String style,

        // 검색 필터용 맛 카테고리 (다중 선택)
        @Schema(description = "맛 카테고리 필터링 (과일, 쌉쌀함 등)", example = "[\"FRUIT\", \"SWEET\"]")
        List<FlavorSearchType> flavor,

        @Schema(description = "베이스", example = "보드카")
        String base,

        @Schema(description = "최소 알코올 도수", example = "0")
        Integer minAbv
) {
    // record는 별도의 생성자나 setter/getter 없이 이대로 바로 사용 가능합니다.
}