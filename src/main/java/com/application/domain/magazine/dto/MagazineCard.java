package com.application.domain.magazine.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 매거진 목록/피드 카드. 응답 JSON camelCase. */
public record MagazineCard(
        Long id,
        String slug,
        String title,
        String dek,
        String subcategory,
        String thumbnail,
        LocalDateTime publishedAt,
        List<String> tags
) {}
