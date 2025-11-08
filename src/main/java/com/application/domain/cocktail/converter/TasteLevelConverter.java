package com.application.domain.cocktail.converter;

import com.application.domain.cocktail.enums.TasteLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TasteLevelConverter implements AttributeConverter<TasteLevel, String> {
    @Override
    public String convertToDatabaseColumn(TasteLevel tasteLevel) {
        return tasteLevel == null ? null : tasteLevel.name();
    }

    @Override
    public TasteLevel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TasteLevel.valueOf(dbData);
    }
}
