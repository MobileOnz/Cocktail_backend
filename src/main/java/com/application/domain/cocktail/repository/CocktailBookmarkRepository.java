package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.CocktailBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CocktailBookmarkRepository extends JpaRepository<CocktailBookmark, Long> {

    /**
     * 특정 유저가 특정 칵테일을 북마크했는지 조회
     */
    Optional<CocktailBookmark> findByMemberIdAndCocktailId(Long memberId, Long cocktailId);

    /**
     * 특정 유저가 북마크한 모든 칵테일 조회 (최근 북마크 순서대로)
     * Cocktail 엔티티도 함께 fetch join으로 가져와 N+1 문제 방지
     */
    @Query("SELECT cb FROM CocktailBookmark cb " +
           "JOIN FETCH cb.cocktail c " +
           "WHERE cb.member.id = :memberId " +
           "ORDER BY cb.createdAt DESC")
    List<CocktailBookmark> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    /**
     * 북마크 여부 확인 (성능 최적화를 위한 boolean 반환)
     */
    boolean existsByMemberIdAndCocktailId(Long memberId, Long cocktailId);
}
