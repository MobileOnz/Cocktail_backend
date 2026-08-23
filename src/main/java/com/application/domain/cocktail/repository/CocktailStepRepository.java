package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.CocktailStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * T-07 리포지토리. 제조 단계 조회 + 도구 매핑 조회 + "만들었어요" 기록을
 * 한 곳에 모았다(cocktail_tool_map / cocktail_made 는 별도 엔티티 없이 네이티브로 처리).
 */
public interface CocktailStepRepository extends JpaRepository<CocktailStep, Long> {

    List<CocktailStep> findByCocktailIdOrderByStepOrderAsc(Long cocktailId);

    @Query(value = "SELECT tool_id FROM cocktail_tool_map WHERE cocktail_id = :cocktailId ORDER BY tool_id", nativeQuery = true)
    List<Long> findToolIdsByCocktailId(@Param("cocktailId") Long cocktailId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM cocktail WHERE id = :id)", nativeQuery = true)
    boolean cocktailExists(@Param("id") Long id);

    /** 멱등 기록: 이미 있으면 무시(ON CONFLICT DO NOTHING). */
    @Modifying
    @Query(value = "INSERT INTO cocktail_made(member_id, cocktail_id) VALUES (:memberId, :cocktailId) ON CONFLICT DO NOTHING", nativeQuery = true)
    int recordMade(@Param("memberId") Long memberId, @Param("cocktailId") Long cocktailId);

    /** '이 칵테일의 이야기' — 연결된 가이드 목록 (part, title, image_url). */
    @Query(value = "SELECT g.part, g.title, g.image_url FROM guide_cocktail_map m " +
            "JOIN guide g ON g.part = m.guide_part WHERE m.cocktail_id = :cocktailId ORDER BY g.part",
            nativeQuery = true)
    List<Object[]> findGuidesByCocktailId(@Param("cocktailId") Long cocktailId);
}
