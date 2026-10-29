package com.application.domain.cocktail.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Same editable dictionary used by CSV normalization and query analysis. */
public final class SearchDictionary {
    private static final JsonNode DATA;
    static {
        try (var in = SearchDictionary.class.getResourceAsStream("/search/dictionary.json")) {
            DATA = new ObjectMapper().readTree(in);
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private SearchDictionary() {}
    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('’', '\'').replace('‘', '\'').replace("ß", "ss").replace("ς", "σ").replaceAll("(?U)\\s+", " ").trim();
    }
    public static String canonical(String group, String value) {
        String n = normalize(value);
        return DATA.path(group).path(n).asText(n);
    }
    public static String ingredient(String value) { return canonical("ingredient_aliases", value); }
    public static String base(String value) { return canonical("base_aliases", value); }
    public static String query(String value) {
        String q = normalize(value);
        for (String group : List.of("base_aliases", "ingredient_aliases", "query_terms")) {
            List<String> keys = new ArrayList<>(); DATA.path(group).fieldNames().forEachRemaining(keys::add);
            keys.sort(Comparator.comparingInt(String::length).reversed());
            for (String key : keys) {
                String canonical = DATA.path(group).path(key).asText();
                Matcher m = Pattern.compile("(?<![\\p{L}])" + Pattern.quote(key)
                        + "(?=$|[^\\p{L}]|(?:과|와|이|가|을|를|은|는)(?:\\s|$))").matcher(q);
                StringBuilder out = new StringBuilder();
                // Skip spots already in canonical form, so "라이 위스키" never becomes "라이 위스키 위스키".
                boolean expands = canonical.startsWith(key);
                while (m.find()) m.appendReplacement(out, Matcher.quoteReplacement(expands && q.startsWith(canonical, m.start()) ? m.group() : canonical));
                q = m.appendTail(out).toString();
            }
        }
        return q;
    }
}
