package com.application.domain.news.dto;

import com.application.domain.news.entity.News;

import java.time.LocalDateTime;

/**
 * 뉴스 목록/피드 카드 (T-08). 필드 camelCase.
 */
public record NewsCard(
        Long id,
        String title,
        String summary,
        String category,
        String categoryLabel,
        String imageUrl,
        String source,
        String sourceUrl,
        LocalDateTime publishedAt,
        Integer viewCount
) {
    public static NewsCard from(News n) {
        return new NewsCard(
                n.getId(),
                n.getTitle(),
                n.getSummary(),
                n.getCategory(),
                NewsCategory.labelOf(n.getCategory()),
                n.getImageUrl(),
                n.getSource(),
                n.getSourceUrl(),
                n.getPublishedAt(),
                n.getViewCount()
        );
    }
}
