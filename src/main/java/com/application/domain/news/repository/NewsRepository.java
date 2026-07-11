package com.application.domain.news.repository;

import com.application.domain.news.entity.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * 커서 페이징 피드. category 가 null 이면 전체, cursor 가 null 이면 처음부터.
     * id 내림차순(=최신순)으로 cursor 미만을 가져온다.
     */
    @Query("SELECT n FROM News n " +
            "WHERE (:category IS NULL OR n.category = :category) " +
            "AND (:cursor IS NULL OR n.id < :cursor) " +
            "ORDER BY n.id DESC")
    List<News> findFeed(@Param("category") String category,
                        @Param("cursor") Long cursor,
                        Pageable pageable);

    List<News> findByFeaturedTrueOrderByPublishedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE News n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
