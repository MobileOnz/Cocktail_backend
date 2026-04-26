package com.application.domain.inquiry.entity;

import java.util.Arrays;
import java.util.Optional;

public enum InquiryStatus {
    NEW,
    READ,
    REPLIED;

    public static Optional<InquiryStatus> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
