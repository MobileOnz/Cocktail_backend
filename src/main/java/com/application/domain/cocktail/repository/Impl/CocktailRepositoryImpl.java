package com.application.domain.cocktail.repository.Impl;


import com.application.domain.cocktail.dto.request.CocktailRecommendationDto;
import com.application.domain.cocktail.dto.request.CocktailSearchConditionDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.QCocktail;
import com.application.domain.cocktail.entity.QCocktailFlavor;
import com.application.domain.cocktail.entity.QCocktailMood;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.FlavorSearchType;
import com.application.domain.cocktail.repository.custom.CocktailRepositoryCustom;
import com.application.domain.cocktail.util.BaseMappingUtil;
import com.application.domain.cocktail.util.FlavorMappingUtil;
import com.application.domain.cocktail.util.FlavorSearchMappingUtil;
import com.application.domain.cocktail.util.MoodMappingUtil;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.JPAExpressions;
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

    /**
     * <pre>
     *     칵테일 검색, 필터링 등을 처리하는 쿼리
     * </pre>
     * @param condition
     * @param pageable
     * @return
     */
    @Override
    public Page<Cocktail> getCocktails(CocktailSearchConditionDto condition, Pageable pageable) {
        QCocktail cocktail = QCocktail.cocktail;

//        System.out.println("condition = " + condition);

        // --- WHERE 절 조립 ---
        BooleanBuilder builder = new BooleanBuilder();

        // 단일 검색창 패턴: korName 또는 engName 어느 쪽으로 들어와도 둘 다 OR 매칭 (case-insensitive).
        // 앱이 항상 korName 만 채워서 보내기 때문에 영문 검색이 안 되던 버그 대응.
        builder.and(nameContainsBilingual(
                StringUtils.hasText(condition.korName()) ? condition.korName() : condition.engName()
        ));
        builder.and(abvBandEq(condition.abvBand()));
        builder.and(styleEq(condition.style()));
//        builder.and(baseEq(condition.base())); // base 다중선택 가능하도록 변경

        // 베이스 교집합(AND) + "기타" OR 처리
        if (!CollectionUtils.isEmpty(condition.base())) {
            for (String baseStr : condition.base()) {
                if ("기타".equals(baseStr)) {
                    BooleanBuilder etcOrBuilder = new BooleanBuilder();
                    for (String etcUnit : BaseMappingUtil.ETC_BASES) {
                        etcOrBuilder.or(cocktail.base.contains(etcUnit));
                    }
                    builder.and(etcOrBuilder);
                } else {
                    builder.and(cocktail.base.contains(baseStr));
                }
            }
        }

        // 맛 카테고리 필터링 로직 (다중 선택)
        List<FlavorSearchType> selectedTypes = condition.flavor();
        if (!CollectionUtils.isEmpty(selectedTypes)) {

            List<String> allDetailTags = new ArrayList<>();

            // [25.12.22] 맛 다중 선택 -> OR 조건에서 AND 조건으로 변경됨
            // 선택된 모든 Enum 타입에 대해 매핑된 세부 태그들을 하나의 리스트로 합칩니다.
//            for (FlavorSearchType type : selectedTypes) {
//                allDetailTags.addAll(FlavorSearchMappingUtil.getTags(type));
//            }
//            // 합쳐진 태그 리스트로 IN 검색 쿼리 추가 (OR 조건으로 동작: 태그 중 하나라도 포함되면 검색)
//            builder.and(flavorNameIn(allDetailTags));

            for (FlavorSearchType type : selectedTypes) {
                // 1. 각 카테고리에 해당하는 세부 태그 리스트 가져오기
                List<String> detailTags = FlavorSearchMappingUtil.getTags(type);

                // 2. 각 카테고리별 조건을 AND로 연결
                // flavorNameIn 메서드는 "해당 태그 리스트 중 하나라도 가진 칵테일 ID인지 확인"하는 서브쿼리를 반환함
                builder.and(flavorNameIn(detailTags));
            }

        }

        // 정렬 조건 리스트 생성
        List<OrderSpecifier<?>> orderSpecifiers = getOrderSpecifiers(pageable.getSort());

        // 1. 컨텐츠 조회 쿼리 (페이징 적용)
        List<Cocktail> content = queryFactory
                .selectFrom(cocktail)
                .distinct()
                .where(builder) // 조립된 WHERE 절 사용
                .orderBy(orderSpecifiers.toArray(OrderSpecifier[]::new)) // 정렬 적용
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리
        Long total = queryFactory
                .select(Wildcard.count)
                .from(cocktail)
                .distinct()
                .where(builder) // 동일한 WHERE 절 사용
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * <pre>
     *     특정 칵테일 조회 쿼리 (입문자, 중급자 등)
     * </pre>
     * @param korNameList
     * @return
     */
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

    /**
     * 맞춤 추천 칵테일 조회 (동적 쿼리)
     * 사용자가 선택한 조건들을 조합하여 후보 칵테일 리스트를 반환합니다.
     */
    @Override
    public List<Cocktail> findRecommendedCocktails(CocktailRecommendationDto dto) {
        BooleanBuilder builder = new BooleanBuilder();

        // 1. 맛 (Flavor) - 단일 선택
        // 사용자가 선택한 Flavor Enum("SWEET")을 실제 DB 값 리스트(["달콤", "꿀"...])로 변환하여 검색
        if (dto.flavor() != null) {
            List<String> detailFlavors = FlavorMappingUtil.getDetailNames(dto.flavor());
            builder.and(flavorNameIn(detailFlavors));
        }

        // 2. 분위기 (Mood) - 단일 선택
        if (dto.mood() != null) {
            List<String> detailMoods = MoodMappingUtil.getDetailNames(dto.mood());
            builder.and(moodNameIn(detailMoods));
        }

        // 3. 계절 (Season) - 단일 선택
        // (계절 상관 X인 경우 null이거나 ALL일 수 있음 - 로직에 따라 처리)
        if (dto.season() != null) {
            // Enum의 description(한글)과 DB 값이 일치한다고 가정
            // 만약 "계절 상관 x" (ALL) 선택 시 조건을 걸지 않으려면 제외
            if (!"계절 상관 x".equals(dto.season().getDescription())) {
                builder.and(cocktail.season.eq(dto.season().getDescription()));
            }
        }

        // 4. 스타일 (Style) - 단일 선택
        if (dto.style() != null) {
            builder.and(cocktail.style.eq(dto.style().getDescription()));
        }

        // 5. 도수 (AbvBand) - 단일 선택
        if (dto.abvBand() != null) {
            // AbvLevelConverter가 적용되어 있다면 Enum 자체로 비교 가능하지만,
            // 안전하게 String 변환 또는 Converter 로직에 맞게 처리
            // 여기서는 Enum의 description("약함")과 DB 값("약함")을 비교
            builder.and(cocktail.abvBand.stringValue().eq(dto.abvBand().getDescription()));
        }

        // 쿼리 실행 (전체 후보군 조회)
        return queryFactory
                .selectFrom(cocktail)
                .distinct()
                .where(builder)
                .fetch();
    }

    /**
     * <pre>
     *     cocktail_flavor 테이블 - 조건 조회
     * </pre>
     * @param flavorNameList
     * @return
     */
//    private BooleanExpression flavorNameIn(List<String> flavorNameList) {
//        if (CollectionUtils.isEmpty(flavorNameList)) {
//            return null;
//        }
//        QCocktailFlavor flavor = QCocktailFlavor.cocktailFlavor;
//        return cocktail.id.in(
//                queryFactory
//                        .select(flavor.cocktail.id)
//                        .from(flavor)
//                        .where(flavor.flavorName.in(flavorNameList))
//                        .groupBy(flavor.cocktail.id)
//                        .fetch()
//        );
//    }

    /**
     * <pre>
     *     cocktail_flavor 테이블 - 조건 조회 (AND 조건 :: 교집합)
     * </pre>
     * @param flavorNameList
     * @return
     */
    private BooleanExpression flavorNameIn(List<String> flavorNameList) {
        if (CollectionUtils.isEmpty(flavorNameList)) {
            return null;
        }

        // 서브쿼리용 Q클래스 별칭 생성 (메인 쿼리의 cocktailFlavor와 겹치지 않게)
        QCocktailFlavor subFlavor = new QCocktailFlavor("subFlavor");

        // 칵테일 ID가 서브쿼리 결과에 포함되는지 확인 (EXISTS와 유사한 효과)
        // SELECT 1 FROM cocktail_flavor WHERE cocktail_id = main.id AND flavor_name IN (...)
        return JPAExpressions
                .selectOne()
                .from(subFlavor)
                .where(subFlavor.cocktail.id.eq(cocktail.id) // 메인 쿼리의 ID와 연결
                        .and(subFlavor.flavorName.in(flavorNameList))) // 태그 조건
                .exists();
    }

    /**
     * <pre>
     *     cocktail_mood 테이블 - 조건 조회
     * </pre>
     * @param moodNameList
     * @return
     */
    private BooleanExpression moodNameIn(List<String> moodNameList) {
        if (CollectionUtils.isEmpty(moodNameList)) {
            return null;
        }
        QCocktailMood mood = QCocktailMood.cocktailMood;
        return cocktail.id.in(
                queryFactory
                        .select(mood.cocktail.id)
                        .from(mood)
                        .where(mood.moodName.in(moodNameList))
                        .groupBy(mood.cocktail.id)
                        .fetch()
        );
    }

    /**
     * <pre>
     *     정렬 메서드?
     * </pre>
     * @param sort
     * @return
     */
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

    /** 한국어/영어 컬럼 둘 다 case-insensitive contains 매칭 (OR). */
    private BooleanExpression nameContainsBilingual(String term) {
        if (!StringUtils.hasText(term)) return null;
        return cocktail.korName.containsIgnoreCase(term)
                .or(cocktail.engName.containsIgnoreCase(term));
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