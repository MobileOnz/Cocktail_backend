package com.application.domain.bar.dto.response;

import com.application.domain.bar.entity.Bar;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 프론트 계약: Cocktail_Front/src/BottomTab/Bar/BarListScreen.tsx `interface BarListItem`
 *   { id, slug, nameKo, nameEn?, distanceKm?, featuredWeight?, updatedAt? }
 *
 * distanceKm 는 **킬로미터**다(FE 가 `.toFixed(1)` 후 "km" 를 붙인다). 미터 아님.
 * 거리 정렬이 아닐 때는 키 자체를 내리지 않는다(FE 가 typeof number 로 분기).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BarListItemDto(
        Long id,
        String slug,
        String nameKo,
        String nameEn,
        String address,
        String heroImage,
        String status,
        Double featuredWeight,
        Double distanceKm,
        LocalDateTime updatedAt
) {
    public static BarListItemDto of(Bar bar, Double distanceKm) {
        return new BarListItemDto(
                bar.getId(), bar.getSlug(), bar.getNameKo(), bar.getNameEn(),
                bar.getAddress(), bar.getHeroImage(), bar.getStatus(),
                bar.getFeaturedWeight(), distanceKm, bar.getUpdatedAt()
        );
    }
}
