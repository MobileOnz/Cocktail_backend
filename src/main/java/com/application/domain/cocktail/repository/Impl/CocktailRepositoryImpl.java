package com.application.domain.cocktail.repository.Impl;


import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.QCocktail;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.repository.custom.CocktailRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.application.domain.cocktail.entity.QCocktail.cocktail;

@RequiredArgsConstructor
public class CocktailRepositoryImpl implements CocktailRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Cocktail> getCocktails(CocktailSearchConditionDto condition, Pageable pageable) {
        QCocktail cocktail = QCocktail.cocktail;

//        System.out.println("condition = " + condition);

        // --- WHERE 절 조립 ---
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(korNameContains(condition.korName()));
        builder.and(engNameContains(condition.engName()));
        builder.and(abvBandEq(condition.abvBand()));
        builder.and(styleEq(condition.style()));
        builder.and(baseEq(condition.base()));

        // 정렬 조건 리스트 생성
        List<OrderSpecifier<?>> orderSpecifiers = getOrderSpecifiers(pageable.getSort());

        // 1. 컨텐츠 조회 쿼리 (페이징 적용)
        List<Cocktail> content = queryFactory
                .selectFrom(cocktail)
                .where(builder) // 조립된 WHERE 절 사용
                .orderBy(orderSpecifiers.toArray(OrderSpecifier[]::new)) // 정렬 적용
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리
        Long total = queryFactory
                .select(Wildcard.count)
                .from(cocktail)
                .where(builder) // 동일한 WHERE 절 사용
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Cocktail> getSpecificCocktails(List<String> korNameList) {
        QCocktail cocktail = QCocktail.cocktail;

        // --- WHERE 절 조립 ---
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(korNameIn(korNameList));

        // 정렬 조건 리스트 생성
//        List<OrderSpecifier<?>> orderSpecifiers = getOrderSpecifiers(pageable.getSort());

        // 1. 컨텐츠 조회 쿼리 (페이징 적용)
        List<Cocktail> content = queryFactory
                .selectFrom(cocktail)
                .where(builder) // 조립된 WHERE 절 사용
                .fetch();

        // 2. 카운트 쿼리
//        Long total = queryFactory
//                .select(Wildcard.count)
//                .from(cocktail)
//                .where(builder) // 동일한 WHERE 절 사용
//                .fetchOne();
//
//        if (total == null) total = 0L;

        return content;
    }

    // Pageable의 Sort 정보를 QueryDSL OrderSpecifier 리스트로 변환하는 메서드
    private List<OrderSpecifier<?>> getOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        // Sort 객체에 있는 모든 정렬 기준을 순회합니다.
        for (Sort.Order order : sort) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String property = order.getProperty(); // 정렬 기준 필드명 (e.g., "korName", "id")

            // 필드명에 따라 해당 Q클래스 컬럼을 OrderSpecifier로 변환
            // (주의: Case 문에서 Q클래스의 필드명을 정확히 매핑해야 합니다.)
            switch (property) {
                case "id":
                    orderSpecifiers.add(new OrderSpecifier(direction, cocktail.id));
                    break;
                case "korName":
                    orderSpecifiers.add(new OrderSpecifier(direction, cocktail.korName));
                    break;
                case "engName":
                    orderSpecifiers.add(new OrderSpecifier(direction, cocktail.engName));
                    break;
                case "minAlcohol":
                    orderSpecifiers.add(new OrderSpecifier(direction, cocktail.minAlcohol));
                    break;
                // case "style": ...
                default:
                    // 기본 정렬 (ID) 또는 아무 정렬도 적용하지 않음
                    orderSpecifiers.add(new OrderSpecifier(Order.ASC, cocktail.id));
                    break;
            }
        }
        // 정렬 조건이 없으면 기본적으로 ID 오름차순 정렬을 추가
        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(new OrderSpecifier(Order.ASC, cocktail.id));
        }
        return orderSpecifiers;
    }

    // --- 💡 동적 쿼리 조각들 (BooleanExpression) ---
    // null이 반환되면 QueryDSL이 알아서 해당 조건을 무시(제거)합니다.

    // ⭐️ 한글 이름 목록 검색 (IN 쿼리)
    private BooleanExpression korNameIn(List<String> korNameList) {
        if (CollectionUtils.isEmpty(korNameList)) {
            return null;
        }
        return cocktail.korName.in(korNameList);
    }

    private BooleanExpression korNameContains(String korName) {
        return StringUtils.hasText(korName) ? cocktail.korName.contains(korName) : null;
    }

    private BooleanExpression engNameContains(String engName) {
        return StringUtils.hasText(engName) ? cocktail.engName.contains(engName) : null;
    }

    // [추가] 도수 레벨 (예: "약함", "보통", "강함") 정확히 일치
    private BooleanExpression abvBandEq(AbvLevel abvBand) {
        return abvBand != null ? cocktail.abvBand.stringValue().eq(abvBand.getAbvLevel()) : null;
    }

    // [추가] 스타일 (예: "클래식", "스트롱", "라이트") 정확히 일치
    private BooleanExpression styleEq(String style) {
        return StringUtils.hasText(style) ? cocktail.style.eq(style) : null;
    }

    // [추가] 베이스 주류 (예: "진", "보드카", "리큐르") 정확히 일치
    private BooleanExpression baseEq(String base) {
        return StringUtils.hasText(base) ? cocktail.base.eq(base) : null;
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