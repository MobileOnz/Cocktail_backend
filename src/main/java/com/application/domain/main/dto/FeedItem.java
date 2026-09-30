package com.application.domain.main.dto;

import com.application.domain.news.dto.NewsCategory;
import com.application.domain.magazine.dto.MagazineCard;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 메인 피드 아이템 (T-09). type 으로 구분되는 판별 유니온.
 * null 필드는 응답에서 제외(@JsonInclude NON_NULL) → 타입별로 필요한 키만 노출.
 *  - type=news  : id,title,summary,category,categoryLabel,imageUrl,publishedAt
 *  - type=guide : part,title,imageUrl
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedItem(
        String type,
        Long id,
        Integer part,
        String title,
        String summary,
        String category,
        String categoryLabel,
        String imageUrl,
        LocalDateTime publishedAt
) {
    /** type 은 앱 호환을 위해 "news" 그대로. id 는 magazine_article.id — 앱이 /api/v2/magazine/{id} 로 연다. */
    public static FeedItem news(MagazineCard c) {
        return new FeedItem("news", c.id(), null, c.title(), c.summary(),
                NewsCategory.codeOf(c.categoryLabel()), c.categoryLabel(), c.imageUrl(), c.publishedAt());
    }

    public static FeedItem guide(Integer part, String title, String imageUrl) {
        return new FeedItem("guide", null, part, title, null, null, null, imageUrl, null);
    }
}
