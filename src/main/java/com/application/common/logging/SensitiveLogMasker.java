package com.application.common.logging;

/** 운영 로그에 남기는 개인정보와 사용자 식별자를 일관된 형식으로 마스킹한다. */
public final class SensitiveLogMasker {

    private static final String EMPTY_VALUE = "-";

    private SensitiveLogMasker() {}

    public static String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return EMPTY_VALUE;
        }
        int[] characters = value.codePoints().toArray();
        if (characters.length <= 4) {
            return "*".repeat(characters.length);
        }
        return new String(characters, 0, 2)
                + "***"
                + new String(characters, characters.length - 2, 2);
    }

    public static String maskMemberId(Long memberId) {
        if (memberId == null) {
            return EMPTY_VALUE;
        }
        String value = Long.toString(memberId);
        int visibleLength = Math.min(2, value.length());
        return "***" + value.substring(value.length() - visibleLength);
    }

    public static String maskName(String value) {
        if (value == null || value.isBlank()) {
            return EMPTY_VALUE;
        }
        int[] characters = value.codePoints().toArray();
        if (characters.length == 1) {
            return "*";
        }
        if (characters.length == 2) {
            return new String(characters, 0, 1) + "*";
        }
        return new String(characters, 0, 1)
                + "*".repeat(characters.length - 2)
                + new String(characters, characters.length - 1, 1);
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return EMPTY_VALUE;
        }
        int separator = email.lastIndexOf('@');
        if (separator <= 0 || separator == email.length() - 1) {
            return maskIdentifier(email);
        }
        String localPart = email.substring(0, separator);
        String domain = email.substring(separator + 1);
        int firstCharacterEnd = localPart.offsetByCodePoints(0, 1);
        return localPart.substring(0, firstCharacterEnd) + "***@" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return EMPTY_VALUE;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return "*".repeat(digits.length());
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }
}
