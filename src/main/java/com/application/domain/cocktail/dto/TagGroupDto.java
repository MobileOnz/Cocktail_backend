package com.application.domain.cocktail.dto;

import com.application.domain.cocktail.enums.TagType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TagGroupDto {
    private TagType type;
    private List<TagDto> tags = new ArrayList<>();

    public TagGroupDto(TagType type) {
        this.type = type;
    }

}
