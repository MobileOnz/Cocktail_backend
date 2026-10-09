package com.application.common.mapper;

import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.dto.IngredientDto;
import com.application.domain.cocktail.dto.TagDto;
import com.application.domain.cocktail.dto.TagGroupDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailTag;
import com.application.domain.cocktail.entity.Ingredient;
import com.application.domain.cocktail.enums.TagType;

import java.util.*;

public class CocktailMapper {
    public static CocktailDto toDto(Cocktail cocktail){
        CocktailDto dto = new CocktailDto();
        dto.setId(cocktail.getId());
        dto.setCocktailEN(firstNonBlank(cocktail.getCocktailEN(), cocktail.getEngName()));
        dto.setCocktailKR(firstNonBlank(cocktail.getCocktailKR(), cocktail.getKorName()));
        dto.setMaxAlcohol(cocktail.getMaxAlcohol());
        dto.setMinAlcohol(cocktail.getMinAlcohol());
        dto.setOriginText(cocktail.getOriginText());
        dto.setImageUrl(cocktail.getImageUrl());
        dto.setAbvBand(cocktail.getAbvBand() == null ? null : cocktail.getAbvBand().name());
        dto.setTasteLevel(cocktail.getTasteLevel() == null ? null : cocktail.getTasteLevel().name());
        dto.setSeasons(cocktail.getSeasons());

        List<IngredientDto> ingredientDtos = new ArrayList<>();
        for(Ingredient ingredient : cocktail.getIngredients()){
            IngredientDto ingredientDto = new IngredientDto(ingredient.getName(), ingredient.getAmount());
            ingredientDtos.add(ingredientDto);
        }
        if (ingredientDtos.isEmpty()
                && cocktail.getIngredientsText() != null
                && !cocktail.getIngredientsText().isBlank()) {
            Arrays.stream(cocktail.getIngredientsText().split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(value -> new IngredientDto(value, ""))
                    .forEach(ingredientDtos::add);
        }
        dto.setIngredients(ingredientDtos);

        dto.setFlavors(cocktail.getFlavors().stream()
                .map(flavor -> flavor.getFlavorName())
                .toList());
        dto.setMoods(cocktail.getMoods().stream()
                .map(mood -> mood.getMoodName())
                .toList());

        // [ {type : flavor, tags : [ "id" : "SWEET" ]} ]
        //TODO: stream() 사용
        Map<TagType, TagGroupDto> tagGroupMaps = new LinkedHashMap<>();
        for(CocktailTag cocktailTag :  cocktail.getTags()){
            TagType type= cocktailTag.getTag().getType();

            TagGroupDto tagGroupDto = tagGroupMaps.get(type);
            //TODO:  computeIfAbsent
            if(tagGroupDto == null){
                tagGroupMaps.put(type, new TagGroupDto(type));
                tagGroupDto = tagGroupMaps.get(type);
            }

            tagGroupDto.getTags().add(new TagDto(cocktailTag.getTag().getId(), cocktailTag.getTag().getName()));
        }
        dto.setTags(new ArrayList<>(tagGroupMaps.values()));

        return dto;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
