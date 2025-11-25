package com.application.domain.cocktail.repository.Impl;


import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.QCocktail;
import com.application.domain.cocktail.repository.custom.CocktailRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class CocktailRepositoryImpl implements CocktailRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Cocktail> getCocktails(CocktailSearchConditionDto condition, Pageable pageable) {
        QCocktail cocktail = QCocktail.cocktail;

        // 1. 컨텐츠 조회 쿼리 (페이징 적용)
        List<Cocktail> content = queryFactory
                .selectFrom(cocktail)
                .where(
                        nameContains(condition.name())       // 이름 검색
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리 (최적화: 조건에 맞는 전체 개수)
        Long total = queryFactory
                .select(Wildcard.count) // count(*)
                .from(cocktail)
                .where(
                        nameContains(condition.name())
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(content, pageable, total);
    }

    // --- 💡 동적 쿼리 조각들 (BooleanExpression) ---
    // null이 반환되면 QueryDSL이 알아서 해당 조건을 무시(제거)합니다.

    private BooleanExpression nameContains(String cocktailKR) {
        return StringUtils.hasText(cocktailKR) ? QCocktail.cocktail.cocktailKR.contains(cocktailKR) : null;
    }

//    private BooleanExpression categoryEq(String category) {
//        return StringUtils.hasText(category) ? QCocktail.cocktail.category.eq(category) : null;
//    }

//    private BooleanExpression primaryLiquorEq(String primaryLiquor) {
//        return StringUtils.hasText(primaryLiquor) ? QCocktail.cocktail.primaryLiquor.eq(primaryLiquor) : null;
//    }

//    private BooleanExpression abvGoe(Integer minAbv) {
//        return minAbv != null ? QCocktail.cocktail.abv.goe(minAbv) : null;
//    }
}