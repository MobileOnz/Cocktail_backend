package com.application.domain.cocktail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema
public class IngredientDto {

    @Schema(description = "재료 이름", example = "콜라")
    private String name;

    @Schema(description = "재료 양", example = "15ml")
    private String amount;

    public IngredientDto(String name, String amount) {
        this.name = name;
        this.amount = amount;
    }
}
