package com.application.domain.cocktail.search;

import java.util.List;

public record HybridSearchRequest(String query, List<String> base, List<String> excludedBase,
        List<String> requiredIngredients, List<String> excludedIngredients,
        Integer abvMin, Integer abvMax, String season, Integer limit, Integer offset, Boolean debug) {}
