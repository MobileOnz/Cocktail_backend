package com.application.domain.bar.dto.response;

import com.application.domain.bar.entity.Bar;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 프론트 계약: Cocktail_Front/src/Screens/Bar/BarDetailScreen.tsx `interface BarDetail`
 *   { id, slug, nameKo, nameEn?, address, lat, lng, phone?, description?,
 *     heroImage?, hours?, status, signatureCocktails: {id,name,image}[], isVisited }
 *
 * null 을 그대로 내려보낸다(FE 가 `?: string | null` 로 선언). NON_NULL 을 붙이지 않는다.
 */
public record BarDetailDto(
        Long id,
        String slug,
        String nameKo,
        String nameEn,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        String phone,
        String description,
        String heroImage,
        Map<String, String> hours,
        String status,
        List<SignatureCocktailDto> signatureCocktails,
        boolean isVisited
) {
    public static BarDetailDto of(Bar bar, List<SignatureCocktailDto> signatures, boolean isVisited) {
        return new BarDetailDto(
                bar.getId(), bar.getSlug(), bar.getNameKo(), bar.getNameEn(),
                bar.getAddress(), bar.getLat(), bar.getLng(), bar.getPhone(),
                bar.getDescription(), bar.getHeroImage(), bar.getHours(), bar.getStatus(),
                signatures, isVisited
        );
    }

    /** FE: `signatureCocktails: Array<{ id: number; name: string; image: string | null }>` */
    public record SignatureCocktailDto(Long id, String name, String image) {}
}
