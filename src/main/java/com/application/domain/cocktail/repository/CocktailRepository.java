package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.repository.custom.CocktailRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface CocktailRepository extends JpaRepository<Cocktail, Long>, CocktailRepositoryCustom {

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
}