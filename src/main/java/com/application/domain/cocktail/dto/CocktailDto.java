package com.application.domain.cocktail.dto;

import com.application.domain.cocktail.enums.Season;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "칵테일 상세 정보 응답 DTO")
public class CocktailDto {

    @Schema(description = "칵테일 고유 ID", example = "1")
    private Long id;

    @Schema(description = "칵테일 국문명", example = "잭콕")
    private String cocktailKR;

    @Schema(description = "칵테일 영문명", example = "JackCock")
    private String cocktailEN;

    @Schema(description = "최대 도수", example = "30")
    private Integer maxAlcohol;

    @Schema(description = "최소 도수", example = "15")
    private Integer minAlcohol;

    @Schema(description = "칵테일 유래 및 설명", example = "잭 + 콜라를 섞은 달달하고..")
    private String originText;

    @Schema(description = "이미지 URL", example = "https://onzcocktails3.s3.amazonaws.com/cocktail/image.jpg")
    private String imageUrl;

    @Schema(description = "도수 레벨 (NON_ALCOHOL, LOW, MIDDLE, HIGH)", example = "MIDDLE")
    private String abvBand;

    @Schema(description = "맛 난이도/레벨 (BEGINNER, INTERMEDIATE, ADVANCED)", example = "BEGINNER")
    private String tasteLevel;

    @Schema(description = "추천 계절 목록", example = "[\"봄\", \"여름\",\"가을\",\"겨울\"")
    private List<Season> seasons;

    @Schema(description = "재료 목록")
    private List<IngredientDto> ingredients;

    @Schema(description = "태그 그룹 목록 (FLAVOR, MOOD, BASE, GLASS)")
    private List<TagGroupDto> tags;
}
