package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.CocktailReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CocktailReactionRepository extends JpaRepository<CocktailReaction, Long> {

    /**
     * 특정 유저가 특정 칵테일에 반응했는지 조회
     */
    Optional<CocktailReaction> findByMemberIdAndCocktailId(Long memberId, Long cocktailId);
}
