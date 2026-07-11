package com.application.domain.news.dto;

import java.util.Map;

/**
 * 뉴스 카테고리 코드 ↔ 한국어 라벨. DB 에는 코드(snake 아님, 대문자)만 저장하고
 * 라벨은 응답 시점에 매핑한다. 프론트 하드코딩 제거용.
 */
public final class NewsCategory {

    private static final Map<String, String> LABELS = Map.of(
            "BEGINNER", "초심자용",
            "EXPERT", "전문가",
            "TREND", "트렌드",
            "WHISKY", "위스키",
            "CARTOON", "만화"
    );

    private NewsCategory() {}

    public static String labelOf(String code) {
        if (code == null) return null;
        return LABELS.getOrDefault(code, code);
    }

    /** "ALL" 또는 null 은 전체(필터 없음)를 의미. 유효 코드가 아니면 null 반환. */
    public static String normalizeFilter(String code) {
        if (code == null || code.isBlank() || "ALL".equalsIgnoreCase(code)) return null;
        return code.toUpperCase();
    }
}
