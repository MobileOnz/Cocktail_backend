package com.application.domain.cocktail.dto.response;

import com.application.domain.cocktail.entity.SearchHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SearchHistoryResponseDto {
    private Long id;
    private String queryText;
    private LocalDateTime createdAt;

    public static SearchHistoryResponseDto from(SearchHistory entity) {
        return SearchHistoryResponseDto.builder()
                .id(entity.getId())
                .queryText(entity.getQueryText())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}