package com.application.domain.cocktail.search;

import java.util.List;

public interface EmbeddingProvider {
    List<double[]> embedDocuments(List<String> texts);
    double[] embedQuery(String query);
    int dimension();
    String modelId();
    String version();
    static void validate(double[] vector, int dimension) {
        if (vector.length != dimension || dimension < 1) throw new IllegalArgumentException("Embedding dimension mismatch");
        double norm = 0;
        for (double v : vector) {
            if (!Double.isFinite(v)) throw new IllegalArgumentException("Non-finite embedding");
            norm += v * v;
        }
        if (!Double.isFinite(norm) || norm <= 0) throw new IllegalArgumentException("Zero/invalid embedding");
    }
}
