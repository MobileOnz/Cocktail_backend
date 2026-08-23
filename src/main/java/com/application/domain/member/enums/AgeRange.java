package com.application.domain.member.enums;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum AgeRange {
    UNDER_19("under_19", "19세 이하"),
    TWENTY_TO_TWENTY_FOUR("20_24", "20-24세"),
    TWENTY_FIVE_TO_TWENTY_NINE("25_29", "25-29세"),
    THIRTY_TO_THIRTY_FOUR("30_34", "30-34세"),
    THIRTY_FIVE_TO_THIRTY_NINE("35_39", "35-39세"),
    OVER_FIFTY("50_over", "50세 이상"),
    // QA P2-7: 프론트 온보딩은 '40세 이상'을 '40_over' 로 보낸다(OnboarindViewModel.ts).
    // 과거엔 40_over 를 OVER_FIFTY(50세 이상)로 오매핑 → 40대가 50대로 분류됐다.
    // @Enumerated 미지정(ORDINAL 저장)이라 기존 순서를 깨지 않도록 반드시 맨 끝에 추가한다.
    OVER_FORTY("40_over", "40세 이상");

    private final String code;
    private final String description;

    AgeRange(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Optional<AgeRange> fromString(String ageRange) {
        if ("19_under".equalsIgnoreCase(ageRange)) {
            return Optional.of(UNDER_19);
        }
        if ("40_over".equalsIgnoreCase(ageRange)) {
            return Optional.of(OVER_FORTY);
        }
        for (AgeRange value : AgeRange.values()) {
            if (value.getCode().equalsIgnoreCase(ageRange)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}