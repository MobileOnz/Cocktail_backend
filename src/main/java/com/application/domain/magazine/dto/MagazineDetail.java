package com.application.domain.magazine.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 매거진 상세. 응답 JSON camelCase.
 * titleLines/content/refs 는 JSONB 원본을 @JsonRawValue 로 그대로 방출(내부 키는 snake_case 유지).
 */
public record MagazineDetail(
        Long id,
        String slug,
        String title,
        @JsonRawValue String titleLines,
        String dek,
        String category,
        String subcategory,
        String heroImage,
        String coverImage,
        String thumbnail,
        String imageCaption,
        String authorName,
        @JsonRawValue String content,
        @JsonRawValue String refs,
        Integer readingTimeMin,
        Integer viewCount,
        LocalDateTime publishedAt,
        List<String> tags
) {}
