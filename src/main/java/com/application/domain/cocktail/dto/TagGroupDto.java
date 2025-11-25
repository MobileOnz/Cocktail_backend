package com.application.domain.cocktail.dto;

import com.application.domain.cocktail.enums.TagType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Schema
public class TagGroupDto {
    @Schema(description = "태그 타입", example = "FLAVOR, MOOD, BASE, GLASS")
    private TagType type;

    @Schema(description = "태그 반환 dto")
    private List<TagDto> tags = new ArrayList<>();

    public TagGroupDto(TagType type) {
        this.type = type;
    }

}
