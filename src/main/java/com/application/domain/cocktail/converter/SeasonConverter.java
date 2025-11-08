package com.application.domain.cocktail.converter;

import com.application.domain.cocktail.enums.Season;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class SeasonConverter implements AttributeConverter<List<Season>, String> {
    @Override
    public String convertToDatabaseColumn(List<Season> seasons) {
        return seasons == null ? null :
                seasons.stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(","));
    }

    @Override
    public List<Season> convertToEntityAttribute(String dbData) {
        return dbData == null ? Collections.emptyList() :
                Arrays.stream(dbData.split(","))
                        .map(Season::valueOf)
                        .toList();
    }
}
