package com.application.common.logging;

/**
 * 장애 분석에 필요한 최소 식별 단서는 남기되 원문 개인정보가 파일에 저장되지 않도록 마스킹한다.
 * Entity 전체를 출력하지 않고 허용된 필드에만 명시적으로 적용한다.
 */
public final class SensitiveLogMasker {

    private static final String EMPTY_VALUE = "-";

    private SensitiveLogMasker() {}

    public static String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return EMPTY_VALUE;
        }
        // 한글·이모지 같은 문자가 중간에서 잘리지 않도록 UTF-16 char가 아닌 code point로 계산한다.
        int[] characters = value.codePoints().toArray();
        if (characters.length <= 4) {
            return "*".repeat(characters.length);
        }
        return new String(characters, 0, 2)
                + "***"
                + new String(characters, characters.length - 2, 2);
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
