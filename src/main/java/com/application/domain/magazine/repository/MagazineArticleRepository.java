package com.application.domain.magazine.repository;

import com.application.domain.magazine.entity.MagazineArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MagazineArticleRepository extends JpaRepository<MagazineArticle, Long> {

    List<MagazineArticle> findByStatusOrderByPublishedAtDesc(String status);

    List<MagazineArticle> findByStatusAndCategoryOrderByPublishedAtDesc(String status, String category);

    Optional<MagazineArticle> findBySlug(String slug);

    @Modifying
    @Query("UPDATE MagazineArticle m SET m.viewCount = m.viewCount + 1 WHERE m.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /** 글에 붙은 태그 라벨(tag.name) 목록. */
    @Query(value = "SELECT t.name FROM magazine_article_tag mat " +
            "JOIN tag t ON t.id = mat.tag_id WHERE mat.article_id = :articleId ORDER BY t.id",
            nativeQuery = true)
    List<String> findTagNames(@Param("articleId") Long articleId);

    @Modifying
    @Query(value = "DELETE FROM magazine_article_tag WHERE article_id = :articleId", nativeQuery = true)
    void clearTags(@Param("articleId") Long articleId);

    @Modifying
    @Query(value = "INSERT INTO magazine_article_tag(article_id, tag_id) VALUES (:articleId, :tagId) " +
            "ON CONFLICT DO NOTHING", nativeQuery = true)
    void addTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);
}
