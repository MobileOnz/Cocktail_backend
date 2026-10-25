package com.application.domain.cocktail.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url, model, version, queryPrefix;
    private final Duration timeout;
    private volatile int dimension;
    public OllamaEmbeddingProvider(
            @Value("${onz.search.embedding.url:http://127.0.0.1:11434}") String url,
            @Value("${onz.search.embedding.model:qwen3-embedding:0.6b}") String model,
            @Value("${onz.search.embedding.version:onz-profile-v1}") String version,
            @Value("${onz.search.embedding.query-prefix:Instruct: Retrieve cocktails matching the requested ingredients, taste and aroma.\nQuery: }") String queryPrefix,
            @Value("${onz.search.embedding.timeout-ms:5000}") long timeoutMs) {
        this.url=url; this.model=model; this.version=version; this.queryPrefix=queryPrefix;
        this.timeout=Duration.ofMillis(timeoutMs);
        this.client=HttpClient.newBuilder().connectTimeout(timeout).build();
    }
    public List<double[]> embedDocuments(List<String> texts) {
        try {
            var request = HttpRequest.newBuilder(URI.create(url + "/api/embed")).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("model", model, "input", texts, "truncate", false)))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()!=200) throw new IllegalStateException("Embedding HTTP " + response.statusCode());
            JsonNode vectors = mapper.readTree(response.body()).path("embeddings");
            if (!vectors.isArray() || vectors.size()!=texts.size()) throw new IllegalStateException("Embedding response count mismatch");
            List<double[]> result = new ArrayList<>();
            for (JsonNode node : vectors) {
                double[] v = mapper.convertValue(node, double[].class);
                synchronized (this) { if (dimension==0) dimension=v.length; }
                EmbeddingProvider.validate(v, dimension);
                result.add(v);
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Embedding interrupted",e);
        } catch (Exception e) { throw new IllegalStateException("Local embedding unavailable", e); }
    }
    public double[] embedQuery(String query) { return embedDocuments(List.of(queryPrefix + query)).get(0); }
    public int dimension() { return dimension; }
    public String modelId() { return model; }
    public String version() { return version; }
}
