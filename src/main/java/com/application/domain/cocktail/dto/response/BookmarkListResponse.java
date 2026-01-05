package com.application.domain.cocktail.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookmarkListResponse {
    private Long memberId;
    private String credentialId;
    private Integer totalCount;
    private List<CocktailResponseDto> cocktails;
}
