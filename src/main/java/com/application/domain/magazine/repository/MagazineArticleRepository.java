package com.application.domain.magazine.repository;

import com.application.domain.magazine.entity.MagazineArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MagazineArticleRepository extends JpaRepository<MagazineArticle, Long> {

    // ── 커서 페이지네이션 ────────────────────────────────────────────────────
    // 정렬 키가 publishedAt 하나뿐이면 같은 시각에 발행된 글에서 커서가 미끄러진다
    // (건너뛰거나 중복된다) → id 를 2차 키로 묶어 keyset 을 유일하게 만든다.
    // category 가 null 이면 전체. 첫 페이지와 다음 페이지를 나눈 이유는,
    // 시각 파라미터를 nullable 로 두면 JPQL 의 널 비교가 방언마다 흔들리기 때문이다.
    // publishedAt IS NOT NULL 은 안전장치다 — 발행분인데 시각이 비어 있으면 커서를 만들 수 없고,
    // Postgres 는 DESC 정렬에서 NULL 을 맨 앞에 놓아 첫 페이지가 통째로 오염된다.

    @Query("SELECT m FROM MagazineArticle m " +
            "WHERE m.status = :status " +
            "AND m.publishedAt IS NOT NULL " +
            "AND (:category IS NULL OR m.category = :category) " +
            "ORDER BY m.publishedAt DESC, m.id DESC")
    List<MagazineArticle> findFirstPage(@Param("status") String status,
                                        @Param("category") String category,
                                        Pageable pageable);

    @Query("SELECT m FROM MagazineArticle m " +
            "WHERE m.status = :status " +
            "AND m.publishedAt IS NOT NULL " +
            "AND (:category IS NULL OR m.category = :category) " +
            "AND (m.publishedAt < :cursorAt OR (m.publishedAt = :cursorAt AND m.id < :cursorId)) " +
            "ORDER BY m.publishedAt DESC, m.id DESC")
    List<MagazineArticle> findNextPage(@Param("status") String status,
                                       @Param("category") String category,
                                       @Param("cursorAt") LocalDateTime cursorAt,
                                       @Param("cursorId") Long cursorId,
                                       Pageable pageable);

    Optional<MagazineArticle> findBySlug(String slug);

    @Modifying
    @Query("UPDATE MagazineArticle m SET m.viewCount = m.viewCount + 1 WHERE m.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /** 글에 붙은 태그 라벨(tag.name) 목록. */
    @Query(value = "SELECT t.name FROM magazine_article_tag mat " +
            "JOIN tag t ON t.id = mat.tag_id WHERE mat.article_id = :articleId ORDER BY t.id",
            nativeQuery = true)
    List<String> findTagNames(@Param("articleId") Long articleId);

    // ── 태그 필터 (네이티브) ─────────────────────────────────────────────────
    // magazine_article_tag / tag 는 @Entity 가 없는 조인 테이블이라 JPQL 로 못 건다.
    // 태그가 주어진 경우에만 쓰며, 정렬·키셋 조건은 JPQL 판과 동일하게 유지한다.
    // :category 는 널일 수 있어 CAST 로 타입을 못박는다(Postgres 가 널 파라미터 타입을 못 정한다).

    @Query(value = "SELECT m.* FROM magazine_article m " +
            "WHERE m.status = :status AND m.published_at IS NOT NULL " +
            "AND (CAST(:category AS varchar) IS NULL OR m.category = CAST(:category AS varchar)) " +
            "AND EXISTS (SELECT 1 FROM magazine_article_tag mat JOIN tag t ON t.id = mat.tag_id " +
            "            WHERE mat.article_id = m.id AND t.name = :tag) " +
            "ORDER BY m.published_at DESC, m.id DESC LIMIT :size", nativeQuery = true)
    List<MagazineArticle> findFirstPageByTag(@Param("status") String status,
                                             @Param("category") String category,
                                             @Param("tag") String tag,
                                             @Param("size") int size);

    @Query(value = "SELECT m.* FROM magazine_article m " +
            "WHERE m.status = :status AND m.published_at IS NOT NULL " +
            "AND (CAST(:category AS varchar) IS NULL OR m.category = CAST(:category AS varchar)) " +
            "AND EXISTS (SELECT 1 FROM magazine_article_tag mat JOIN tag t ON t.id = mat.tag_id " +
            "            WHERE mat.article_id = m.id AND t.name = :tag) " +
            "AND (m.published_at < :cursorAt OR (m.published_at = :cursorAt AND m.id < :cursorId)) " +
            "ORDER BY m.published_at DESC, m.id DESC LIMIT :size", nativeQuery = true)
    List<MagazineArticle> findNextPageByTag(@Param("status") String status,
                                            @Param("category") String category,
                                            @Param("tag") String tag,
                                            @Param("cursorAt") LocalDateTime cursorAt,
                                            @Param("cursorId") Long cursorId,
                                            @Param("size") int size);

    /** 발행분에 실제로 붙어 있는 태그를 많이 쓰인 순으로. 목록 화면의 필터 칩 재료. */
    @Query(value = "SELECT t.name FROM magazine_article_tag mat " +
            "JOIN tag t ON t.id = mat.tag_id " +
            "JOIN magazine_article m ON m.id = mat.article_id " +
            "WHERE m.status = 'PUBLISHED' AND m.published_at IS NOT NULL " +
            "GROUP BY t.name ORDER BY count(*) DESC, t.name ASC", nativeQuery = true)
    List<String> findPublishedTagNames();

    @Modifying
    @Query(value = "DELETE FROM magazine_article_tag WHERE article_id = :articleId", nativeQuery = true)
    void clearTags(@Param("articleId") Long articleId);

    @Modifying
    @Query(value = "INSERT INTO magazine_article_tag(article_id, tag_id) VALUES (:articleId, :tagId) " +
            "ON CONFLICT DO NOTHING", nativeQuery = true)
    void addTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);
}
