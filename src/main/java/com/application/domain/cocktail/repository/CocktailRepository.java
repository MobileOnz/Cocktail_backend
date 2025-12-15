package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.repository.custom.CocktailRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CocktailRepository extends JpaRepository<Cocktail, Long>, CocktailRepositoryCustom {

    /**
     * <pre>
     *     [JpaRepository 메서드 쿼리]
     *     특정 문자열로 시작하는 칵테일 최대 5개 조회
     * </pre>
     * @param searchText 검색어
     */
    List<CocktailNameProjection> findTop5ByKorNameStartingWith(String searchText);

    /**
     * <pre>
     *     recommendCount 기준으로 내림차순 정렬하여 상위 10개의 칵테일 엔티티를 조회합니다.
     *     findTop10By : 상위 10개 제한 (LIMIT 10)
     *     OrderByRecommendCountDesc : recommendCount 기준 내림차순 정렬 (ORDER BY recommend_count DESC)
     * </pre>
     */
    List<Cocktail> findTop10ByOrderByRecommendCountDesc();

    /**
     * <pre>
     *     최신순 limit 10
     * </pre>
     * @return
     */
    List<Cocktail> findTop10ByOrderByUpdatedAtDesc();

    // 동시성을 고려한 Atomic 증가

    // 추천해요 증가 (DB 직접 연산)
    @Modifying(clearAutomatically = true) // 연산 후 영속성 컨텍스트 초기화
    @Query("UPDATE Cocktail c SET c.recommendCount = c.recommendCount + 1 WHERE c.id = :id")
    void incrementRecommend(@Param("id") Long id);

    // 추천해요 감소
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Cocktail c SET c.recommendCount = c.recommendCount - 1 WHERE c.id = :id")
    void decrementRecommend(@Param("id") Long id);

    // 어려워요 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Cocktail c SET c.hardCount = c.hardCount + 1 WHERE c.id = :id")
    void incrementHard(@Param("id") Long id);

    // 어려워요 감소
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Cocktail c SET c.hardCount = c.hardCount - 1 WHERE c.id = :id")
    void decrementHard(@Param("id") Long id);

    /**
     * 칵테일 이름(korName)만 가져오기 위한 Projection Interface
     * get필드명() 메서드를 정의하면 JPA가 해당 필드만 SELECT하여 채워줍니다.
     */
    interface CocktailNameProjection {
        String getKorName();
    }
}