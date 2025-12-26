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
    OVER_FIFTY("50_over", "50세 이상");

    private final String code;
    private final String description;

    AgeRange(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Optional<AgeRange> fromString(String ageRange) {
        for (AgeRange value : AgeRange.values()) {
            if (value.getCode().equalsIgnoreCase(ageRange)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}