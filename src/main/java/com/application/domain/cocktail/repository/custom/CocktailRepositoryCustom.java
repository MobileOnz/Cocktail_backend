package com.application.domain.cocktail.repository.custom;

import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.entity.Cocktail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CocktailRepositoryCustom {
    // 동적 검색 메서드 정의
    Page<Cocktail> getCocktails(CocktailSearchConditionDto condition, Pageable pageable);
}