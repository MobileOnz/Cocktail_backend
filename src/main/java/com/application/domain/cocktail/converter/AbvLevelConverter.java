package com.application.domain.cocktail.converter;

import com.application.domain.cocktail.enums.AbvLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AbvLevelConverter implements AttributeConverter<AbvLevel, String>{

    //쓰기
    @Override
    public String convertToDatabaseColumn(AbvLevel abvLevel) {
        return abvLevel == null ? null : abvLevel.name();
    }

    //읽기
    @Override
    public AbvLevel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AbvLevel.valueOf(dbData);
    }
}
