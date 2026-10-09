package com.application.domain.cocktail.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.CocktailDto;
import com.application.domain.cocktail.dto.IngredientDto;
import com.application.domain.cocktail.service.CocktailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 구 버전 앱의 레시피북이 사용하는 단건 조회 API.
 *
 * <p>현재 앱은 v2 API를 사용하지만, 이미 설치된 구 버전은 이 경로에서 {@code data.cocktail}
 * 구조를 읽는다. 기존 {@link CocktailDto} 필드도 함께 내려 이전 클라이언트 계약을 유지한다.</p>
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "구 앱 호환 칵테일 API")
public class LegacyCocktailController {

    private final CocktailService cocktailService;

    @Operation(summary = "구 앱 레시피북 칵테일 단건 조회")
    @GetMapping("/cocktail")
    public ResponseEntity<ResponseDto<Map<String, Object>>> getCocktail(
            @RequestParam Long cocktailId) {
        CocktailDto dto = cocktailService.getCocktailInfo(cocktailId);

        Map<String, Object> cocktail = new LinkedHashMap<>();
        cocktail.put("id", dto.getId());
        cocktail.put("cocktail_name", dto.getCocktailKR());
        cocktail.put("cocktail_name_en", dto.getCocktailEN());
        cocktail.put("introduce", dto.getOriginText());
        cocktail.put("cocktail_size", 0);
        cocktail.put("min_alchol", dto.getMinAlcohol());
        cocktail.put("max_alchol", dto.getMaxAlcohol());
        cocktail.put("image_url", dto.getImageUrl());

        List<Map<String, String>> ingredients = safeList(dto.getIngredients()).stream()
                .map(ingredient -> Map.of(
                        "ingredient", ingredient.getName(),
                        "quantity", ingredient.getAmount() == null ? "" : ingredient.getAmount(),
                        "unit", ""
                ))
                .toList();

        List<Map<String, String>> tastes = safeList(dto.getFlavors()).stream()
                .map(flavor -> Map.of(
                        "tasteDetail", flavor,
                        "category", ""
                ))
                .toList();

        List<Map<String, String>> recommends = safeList(dto.getMoods()).stream()
                .map(mood -> Map.of("mood", mood))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", dto.getId());
        data.put("cocktailKR", dto.getCocktailKR());
        data.put("cocktailEN", dto.getCocktailEN());
        data.put("maxAlcohol", dto.getMaxAlcohol());
        data.put("minAlcohol", dto.getMinAlcohol());
        data.put("originText", dto.getOriginText());
        data.put("imageUrl", dto.getImageUrl());
        data.put("abvBand", dto.getAbvBand());
        data.put("tasteLevel", dto.getTasteLevel());
        data.put("seasons", safeList(dto.getSeasons()));
        data.put("ingredients", ingredients);
        data.put("tastes", tastes);
        data.put("recommends", recommends);
        data.put("cocktail", cocktail);

        return ResponseEntity.ok(ResponseDto.onSuccess("cocktail info", data));
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
