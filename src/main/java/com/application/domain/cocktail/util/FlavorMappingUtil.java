package com.application.domain.cocktail.util;

import com.application.domain.cocktail.enums.recommendation.Flavor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 상위 Flavor 카테고리(Enum)를 하위 세부 맛 이름(DB 값) 리스트로 매핑하는 유틸리티 클래스.
 * [주의] 현재는 빠른 구현을 위해 하드코딩되었으며, 맛이 추가되면 수동으로 코드를 업데이트해야 합니다.
 * FIXME 테이블 구조를 수정하여 하드코딩은 없애야 함
 */
public class FlavorMappingUtil {

    // EnumMap을 사용하여 Flavor Enum과 세부 맛 목록을 맵핑
    private static final Map<Flavor, List<String>> FLAVOR_DETAIL_MAP;

    static {
        // 불변(Immutable) Map을 생성하여 스레드 안전성을 확보합니다.
        FLAVOR_DETAIL_MAP = Collections.unmodifiableMap(new EnumMap<>(Flavor.class) {{

            // 1. SWEET (달콤한 맛)
            put(Flavor.SWEET, List.of("달콤", "달콤한", "단맛", "꿀", "설탕", "그레나딘 레이어", "은은한 단맛", "달콤상큼", "쌉쌀달콤", "부드러운", "스위트"));

            // 2. SPARKLING (청량 스파클링)
            put(Flavor.SPARKLING, List.of("청량", "청량감", "탄산", "스파클링", "토닉", "톡쏘는", "거품", "부드러운 거품", "상쾌함", "시원한", "에너지", "콜라"));

            // 3. CITRUS (상큼 시트러스)
            put(Flavor.CITRUS, List.of("상큼", "상큼한", "상큼함", "시트러스", "레몬", "라임", "자몽", "오렌지", "오렌지 향", "새콤", "새콤달콤", "깔끔한"));

            // 4. TROPICAL (과일향 트로피컬)
            put(Flavor.TROPICAL, List.of("열대과일", "트로피컬", "열대", "파인애플", "코코넛", "바나나", "복숭아", "베리", "블랙베리", "크랜베리", "패션프루트", "패션후르츠", "포도", "포도향", "체리", "체리 향", "체리향", "살구", "애플", "풍부한 과일", "과일향", "과일", "프루티", "라즈베리"));

            // 5. BITTER (쌉싸름 비터)
            put(Flavor.BITTER, List.of("쌉싸름", "쌉쌀한", "드라이", "묵직함", "묵직한", "균형", "밸런스", "클래식")); // '균형', '클래식'은 전통적으로 쓴맛 계열에 통합

            // 6. SPICY (스파이시 따뜻한)
            put(Flavor.SPICY, List.of("향신", "향신료", "따뜻함", "스파이시", "생강", "칠리"));

            // 7. HERBAL (허브 프레시)
            put(Flavor.HERBAL, List.of("허브", "허브향", "약초", "바질", "민트", "캐모마일", "프레시", "꽃향기", "꽃향", "플로럴"));

            // 분류되지 않은 기타 맛 (선택 사항: 필요한 경우 별도 처리)
            // UNKNOWNs: 소금 림, 코코아, 크리미, 너티, 강한, 매우 강함, 시각적, 위스키, 레드와인, 와인 등
        }});
    }

    /**
     * 특정 상위 Flavor 카테고리(Enum)에 속하는 모든 세부 맛 이름 리스트를 반환합니다.
     * @param flavor 상위 Flavor Enum (SWEET, CITRUS 등)
     * @return 해당 카테고리에 속하는 세부 맛 이름(String) 리스트
     */
    public static List<String> getDetailNames(Flavor flavor) {
        // Map에서 Flavor Enum에 해당하는 리스트를 가져오거나, 없으면 빈 리스트 반환
        return FLAVOR_DETAIL_MAP.getOrDefault(flavor, Collections.emptyList());
    }
}