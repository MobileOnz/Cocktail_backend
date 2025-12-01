package com.application.domain.cocktail.converter;

import com.application.domain.cocktail.enums.AbvLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/**
 * AbvLevel Enum <-> DB String ("약함", "보통", "강함") 양방향 변환을 위한 Converter
 * * 목적: DB에 저장된 한글 값(약함)과 Enum 상수(WEAK)를 연결합니다.
 */
@Converter(autoApply = true)
public class AbvLevelConverter implements AttributeConverter<AbvLevel, String> {

    /**
     * DB에 저장할 때 (Java 객체 -> DB 한글 문자열)
     */
    @Override
    public String convertToDatabaseColumn(AbvLevel abvLevel) {
        if (abvLevel == null) {
            return null;
        }
        // DB에는 AbvLevel Enum이 가진 한글 필드 값 (e.g., "약함")을 저장합니다.
        // 사용자의 AbvLevel Enum 정의에 따라 getAbvLevel()을 사용합니다.
        return abvLevel.getAbvLevel();
    }

    /**
     * DB에서 읽어올 때 (DB 한글 문자열 -> Java 객체)
     */
    @Override
    public AbvLevel convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        // DB 문자열 ("약함")과 일치하는 Enum 상수를 찾아서 반환합니다.
        return Arrays.stream(AbvLevel.values())
                .filter(level -> level.getAbvLevel().equals(dbData))
                .findFirst()
                // DB에 '알 수 없는 한글값'이 들어있으면 예외 발생 (데이터 무결성 검증)
                .orElseThrow(() -> new IllegalArgumentException("Unknown AbvLevel data in DB: " + dbData));
    }
}