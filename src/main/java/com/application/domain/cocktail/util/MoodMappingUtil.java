package com.application.domain.cocktail.util;

import com.application.domain.cocktail.enums.recommendation.Mood;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * 상위 Mood 카테고리(Enum)를 하위 세부 분위기 이름(DB 값) 리스트로 매핑하는 유틸리티 클래스.
 */
public class MoodMappingUtil {

    private static final Map<Mood, List<String>> MOOD_DETAIL_MAP;

    static {
        // 불변(Immutable) Map을 생성하여 스레드 안전성을 확보합니다.
        MOOD_DETAIL_MAP = Collections.unmodifiableMap(new EnumMap<>(Mood.class) {{

            // 1. MEAL_TIME (식전 식후) - 식사 관련, 시간대
            put(Mood.MEAL_TIME, List.of(
                    "식전주", "식후", "디너 후", "가벼운 저녁", "식전", "저녁 식전주", "브런치", "데이타임",
                    "오후", "오후의 휴식", "해피아워", "여름 저녁", "여름 오후", "해장", "낮", "밤", "깊은 밤"
            ));

            // 2. ROMANTIC (데이트 로맨틱) - 감성적, 여성적
            put(Mood.ROMANTIC, List.of(
                    "데이트 로맨틱", "로맨틱", "아름다운", "우아한", "기념일", "여성적인", "여성 인기",
                    "세련된", "화사함", "은밀한", "조용한 밤", "향긋한"
            ));

            // 3. PARTY (파티 여럿이) - 활기참, 모임, 여행, 휴양
            put(Mood.PARTY, List.of(
                    "파티", "파티 여럿이", "축하", "축하 자리", "야외파티", "여름 파티", "홈파티", "가벼운 파티",
                    "사교 모임", "특별한 날", "열정적인", "화려한", "강렬한", "위험한",
                    // 여행/휴양/남미 계열 (이국적이고 활기찬 분위기)
                    "남미", "멕시코", "아르헨티나", "브라질", "여행지", "휴양지", "바캉스", "해변", "풀사이드",
                    "이국적인", "티키 스타일", "티키", "열대", "트로피컬"
            ));

            // 4. CASUAL (집에서 간단히) - 편안함, 일상적
            put(Mood.CASUAL, List.of(
                    "집에서 간단히", "혼술", "캐주얼", "캐주얼 모임", "캐주얼 바", "느긋한 시간", "가벼운", "차분한",
                    "리프레시", "상쾌한", "상큼한", "깔끔한", "대중적인", "균형감", "봄", "여름", "가을", "겨울", "추운 날", "더운 날"
            ));

            // 5. MODERN (세련된 모던) - 현대적, 도시적, 독특함
            put(Mood.MODERN, List.of(
                    "세련된 모던", "도시적인", "트렌디", "현대적인", "퓨전", "개성있는", "독특한",
                    "각성", "지적인", "진중한", "깊은 대화", "고급", "스파이", "스모키", "복합적인"
            ));

            // 6. CLASSIC (클래식 전통) - 역사적, 중후함, 유럽 전통
            put(Mood.CLASSIC, List.of(
                    "클래식 전통", "클래식", "클래식 바", "전통", "중후한", "깊은 맛", "역사적인", "레트로", "70년대",
                    "전환기", "현지", "정성", "남성적인", "경마",
                    // 유럽/역사적 지역 (전통적인 칵테일 문화)
                    "프랑스", "이탈리아", "아일랜드", "뉴올리언스"
            ));

        }});
    }

    public static List<String> getDetailNames(Mood mood) {
        return MOOD_DETAIL_MAP.getOrDefault(mood, Collections.emptyList());
    }
}