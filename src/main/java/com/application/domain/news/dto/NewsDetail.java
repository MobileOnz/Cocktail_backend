package com.application.domain.news.dto;

import com.application.domain.news.entity.News;

import java.time.LocalDateTime;

/**
 * 뉴스 상세 (T-08). NewsCard + content. 필드 camelCase.
 */
public record NewsDetail(
        Long id,
        String title,
        String summary,
        String content,
        String category,
        String categoryLabel,
        String imageUrl,
        String source,
        String sourceUrl,
        LocalDateTime publishedAt,
        Integer viewCount
) {
    public static NewsDetail from(News n) {
        return new NewsDetail(
                n.getId(),
                n.getTitle(),
                n.getSummary(),
                n.getContent(),
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
