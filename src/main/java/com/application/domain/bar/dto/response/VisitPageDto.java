package com.application.domain.bar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프론트 계약: Cocktail_Front/src/Screens/Bar/VisitedBarsScreen.tsx
 *   data = { items: VisitItem[], nextCursor: number|null }
 *   VisitItem = { id, visitedAt, bar: { id, slug, nameKo } }
 *
 * nextCursor 는 null 을 명시적으로 내려보낸다(FE 가 `nextCursor === null` 로 끝을 판정).
 */
public record VisitPageDto(
        List<VisitItemDto> items,
        Long nextCursor
) {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record VisitItemDto(
            Long id,
            LocalDateTime visitedAt,
            BarBriefDto bar
    ) {}

    public record BarBriefDto(Long id, String slug, String nameKo) {}
}
