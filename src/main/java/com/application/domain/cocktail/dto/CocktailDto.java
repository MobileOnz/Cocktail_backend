package com.application.domain.cocktail.dto;

import com.application.domain.cocktail.enums.Season;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CocktailDto {
    private Long id;
    private String cocktailKR;
    private String cocktailEN;
    private Integer maxAlcohol;
    private Integer minAlcohol;
    private String originText;
    private String imageUrl;
    private String abvBand;
    private String tasteLevel;
    private List<Season> seasons;

    private List<IngredientDto> ingredients;
    private List<TagGroupDto> tags;
}
