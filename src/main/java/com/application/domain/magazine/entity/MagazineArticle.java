package com.application.domain.magazine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 매거진 글 (V10). magazine_article 테이블과 매핑.
 * content/refs/title_lines 는 JSONB 원본 문자열로 통과 저장(파싱하지 않음 → 스키마 유연성).
 * 컬럼 snake_case, 응답 JSON 은 DTO 에서 camelCase (기존 v2 관례).
 */
@Entity
@Table(name = "magazine_article")
@Getter
@Setter
@NoArgsConstructor
public class MagazineArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title_lines", columnDefinition = "jsonb")
    private String titleLines;

    private String dek;

    @Column(nullable = false)
    private String category;

    private String subcategory;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "hero_image")
    private String heroImage;

    private String thumbnail;

    @Column(name = "image_caption")
    private String imageCaption;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_bio")
    private String authorBio;

    @Column(name = "author_avatar")
    private String authorAvatar;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String refs;

    @Column(name = "reading_time_min")
    private Integer readingTimeMin;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
