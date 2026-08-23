package com.application.domain.magazine.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 매거진 목록 카드. 앱의 기존 뉴스 카드와 필드명을 맞춰(imageUrl/categoryLabel/source ...)
 * 목록 화면이 URL 교체만으로 동작하게 한다. 응답 JSON camelCase.
 */
public record MagazineCard(
        Long id,
        String title,
        String summary,        // dek
        String category,       // STORY | MOOD | BASE | SEASON
        String categoryLabel,  // subcategory (칩 라벨)
        String imageUrl,       // thumbnail
        String source,         // author_name
        String sourceUrl,      // 항상 null (호환용)
        LocalDateTime publishedAt,
        Integer viewCount,
        List<String> tags
) {}
