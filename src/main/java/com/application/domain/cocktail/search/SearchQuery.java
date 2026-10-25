package com.application.domain.cocktail.search;

import java.util.*;
import java.util.regex.*;

record SearchQuery(String text, List<String> terms, Set<String> bases, Set<String> excludedBases,
        Set<String> required, Set<String> excluded, Integer abvMin, Integer abvMax, String season, boolean filterOnly) {
    static SearchQuery analyze(HybridSearchRequest r, Set<String> entities, Set<String> knownBases) {
        String q = SearchDictionary.query(r.query());
        Set<String> bases = normalized(r.base(), true), noBases = normalized(r.excludedBase(), true);
        Set<String> required = normalized(r.requiredIngredients(), false), excluded = normalized(r.excludedIngredients(), false);
        Set<String> hits = new LinkedHashSet<>();
        // Longest entity wins, so 진저 비어 / 화이트 럼 never split into unintended entities.
        String remaining = q;
        List<String> sorted = new ArrayList<>(entities);
        sorted.addAll(knownBases);
        sorted = sorted.stream().distinct().sorted(Comparator.comparingInt(String::length).reversed()).toList();
        for (String entity : sorted) {
            Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}])"+Pattern.quote(entity)+"(?=$|[^\\p{L}\\p{N}]|(?:과|와|이|가|을|를|은|는)(?:\\s|$))");
            Matcher m = pattern.matcher(remaining);
            if (!m.find()) continue;
            String tail = remaining.substring(m.end());
            if (tail.matches("^(?:을|를|은|는|이|가)?\\s*(?:없이|빼고|제외).*$")) {
                excluded.add(entity);
                if (knownBases.contains(entity)) noBases.add(entity);
            } else {
                hits.add(entity);
                if (knownBases.contains(entity) && tail.matches("^\\s*베이스.*$")) bases.add(entity);
                if (q.matches(".*(?:들어간|포함된|포함한).*")) required.add(entity);
            }
            remaining = m.replaceAll(" ");
        }
        Set<String> terms = new LinkedHashSet<>(hits);
        for (String token : q.split("\\s+")) if (token.length() > 1) terms.add(token);
        terms.removeAll(excluded);
        // Weak preference from explicit tags only; never a numerical sweetness guarantee/filter.
        if (q.matches(".*(?:달지|안 달|덜 달).*")) terms.addAll(List.of("드라이", "쌉싸름"));
        Integer min=r.abvMin(), max=r.abvMax();
        Matcher abv = Pattern.compile("도수\\s*(\\d{1,3})\\s*도\\s*(이하|이상)").matcher(q);
        while (abv.find()) {
            int value=Integer.parseInt(abv.group(1));
            if (value > 100) throw new IllegalArgumentException("도수는 0~100이어야 합니다");
            if (abv.group(2).equals("이하")) max=max==null?value:Math.min(max,value);
            else min=min==null?value:Math.max(min,value);
        }
        boolean filters = !bases.isEmpty() || !noBases.isEmpty() || !required.isEmpty() || !excluded.isEmpty() || min!=null || max!=null || r.season()!=null;
        return new SearchQuery(q,List.copyOf(terms),bases,noBases,required,excluded,min,max,
                r.season()==null?null:SearchDictionary.normalize(r.season()),filters && hits.isEmpty());
    }
    private static Set<String> normalized(List<String> values, boolean base) {
        Set<String> out = new LinkedHashSet<>();
        if (values!=null) values.forEach(v -> out.add(base?SearchDictionary.base(v):SearchDictionary.ingredient(v)));
        return out;
    }
}
