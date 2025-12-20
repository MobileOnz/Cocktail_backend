package com.application.domain.cocktail.util;

import com.application.domain.cocktail.enums.FlavorSearchType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FlavorSearchMappingUtil {

    private static final Map<FlavorSearchType, List<String>> FLAVOR_TAG_MAP;

    static {
        Map<FlavorSearchType, List<String>> map = new EnumMap<>(FlavorSearchType.class);

        // 1. 과일 (FRUIT)
        map.put(FlavorSearchType.FRUIT, List.of(
                "과일", "과일향", "상큼", "상큼한", "상큼함", "새콤", "새콤달콤", "달콤상큼",
                "레몬", "라임", "자몽", "오렌지", "오렌지 향", "파인애플", "코코넛", "바나나",
                "복숭아", "베리", "블랙베리", "크랜베리", "라즈베리", "체리", "체리 향", "체리향",
                "살구", "애플", "포도", "포도향", "패션프루트", "패션후르츠", "풍부한 과일",
                "시트러스", "트로피컬", "열대", "열대과일", "프루티"
        ));

        // 2. 쌉쌀함 (BITTER)
        map.put(FlavorSearchType.BITTER, List.of(
                "쌉싸름", "쌉쌀한", "드라이", "달콤쌉쌀", "쌉쌀달콤"
        ));

        // 3. 달콤함 (SWEET)
        map.put(FlavorSearchType.SWEET, List.of(
                "달콤", "달콤한", "단맛", "꿀", "설탕", "스위트", "은은한 단맛", "그레나딘 레이어"
        ));

        // 4. 부드러움 (CREAMY)
        map.put(FlavorSearchType.CREAMY, List.of(
                "부드러운", "크리미", "거품", "부드러운 거품", "바닐라", "코코아", "커피",
                "너티", "아몬드"
        ));

        // 5. 복합적인 맛 (COMPLEX)
        map.put(FlavorSearchType.COMPLEX, List.of(
                "복합적인", "균형", "밸런스"
        ));

        // 6. 허브 & 스파이스 (HERBAL_SPICE)
        map.put(FlavorSearchType.HERBAL_SPICE, List.of(
                "허브", "허브향", "약초", "바질", "민트", "캐모마일", "꽃향", "꽃향기", "플로럴",
                "스파이시", "향신", "향신료", "생강", "칠리", "아니스", "아가베", "따뜻함"
        ));

        // 7. 라이트 & 청량함 (LIGHT_REFRESHING)
        map.put(FlavorSearchType.LIGHT_REFRESHING, List.of(
                "라이트", "청량", "청량감", "탄산", "스파클링", "토닉", "콜라", "시원한",
                "상쾌함", "상쾌한", "깔끔한", "가벼움", "프레시", "톡쏘는", "화사함"
        ));

        // 8. 개성 강한 맛 (STRONG_UNIQUE)
        map.put(FlavorSearchType.STRONG_UNIQUE, List.of(
                "개성있는", "독특한", "강한", "매우 강함", "묵직함", "묵직한", "스모키",
                "짭짤한", "짭짤함", "소금 림", "위스키", "레드와인", "와인", "셰리", "토마토"
        ));

        // 9. 기타 & 특별한 맛 (ETC_SPECIAL)
        map.put(FlavorSearchType.ETC_SPECIAL, List.of(
                "클래식", "트렌디", "시각적", "에너지", "브라질"
        ));

        FLAVOR_TAG_MAP = Collections.unmodifiableMap(map);
    }

    public static List<String> getTags(FlavorSearchType type) {
        return FLAVOR_TAG_MAP.getOrDefault(type, Collections.emptyList());
    }
}