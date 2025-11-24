package com.application.domain.cocktail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class TagDto {

    @Schema(description = "Tag 아이디", example = "1L")
    private Long id;

    @Schema(description = "Tag 이름")
    private String name;

    public TagDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
