package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CocktailTagRepository extends JpaRepository<CocktailTag, Long> {
    @Query(value = "SELECT DISTINCT c.* FROM cocktail c " +
            "JOIN cocktail_tag ct ON c.id = ct.cocktail_id " +
            "JOIN tag t ON ct.tag_id = t.id " +
            "WHERE t.id IN (:tagIds)", nativeQuery = true)
    List<Cocktail> findByTagIds(@Param("tagIds") List<Long> tagId);
}
