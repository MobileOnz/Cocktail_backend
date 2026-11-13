package com.application.domain.cocktail.search;

import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import java.util.*;

/** Explicit local batch command; never runs during application startup or a public search. */
public final class SearchIndexer {
    private final JdbcTemplate jdbc;
    private final EmbeddingProvider provider;
    private final ObjectMapper mapper=new ObjectMapper();
    private final TransactionTemplate transaction;
    public SearchIndexer(JdbcTemplate jdbc, EmbeddingProvider provider) {
        this.jdbc=jdbc; this.provider=provider;
        transaction=new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(jdbc.getDataSource())));
    }
    public Map<String,Integer> index(List<JsonNode> documents, boolean dryRun, boolean force, int limit, int batchSize) {
        if (limit<1||batchSize<1||batchSize>100) throw new IllegalArgumentException("Invalid batch size/limit");
        List<JsonNode> pending=new ArrayList<>();
        int inspected=0;
        for (JsonNode doc:documents) {
            if (inspected++>=limit) break;
            long id=doc.path("id").asLong();
            var names=jdbc.queryForList("SELECT kor_name,eng_name FROM cocktail WHERE id=?",id);
            if (names.size()!=1 || !SearchDictionary.normalize((String)names.get(0).get("kor_name")).equals(SearchDictionary.normalize(doc.path("name_ko").asText()))
                    || !SearchDictionary.normalize((String)names.get(0).get("eng_name")).equals(SearchDictionary.normalize(doc.path("name_en").asText())))
                throw new IllegalStateException("CSV/DB identity mismatch for cocktail " + id + "; original records were not modified");
            var previous=jdbc.queryForList("SELECT content_hash,embedding_model,embedding_version FROM cocktail_search_document WHERE cocktail_id=?",id);
            if (force||previous.isEmpty()||!previous.get(0).get("content_hash").equals(doc.path("content_hash").asText())
                    ||!previous.get(0).get("embedding_model").equals(provider.modelId())||!previous.get(0).get("embedding_version").equals(provider.version())) pending.add(doc);
        }
        if (!dryRun) for (int start=0;start<pending.size();start+=batchSize) {
            var batch=pending.subList(start,Math.min(start+batchSize,pending.size()));
            List<double[]> embeddings=provider.embedDocuments(batch.stream().map(d->d.path("profile_text").asText()).toList());
            if (embeddings.size()!=batch.size()) throw new IllegalStateException("Embedding batch size mismatch");
            for (double[] vector:embeddings) EmbeddingProvider.validate(vector,provider.dimension());
            transaction.executeWithoutResult(status->{
                for (int i=0;i<batch.size();i++) upsert(batch.get(i),embeddings.get(i));
            });
            System.out.println("Indexed batch: " + batch.stream().map(d->d.path("id").asLong()).toList());
        }
        return Map.of("examined",Math.min(documents.size(),limit),"changed",pending.size(),"indexed",dryRun?0:pending.size(),"unchanged",Math.min(documents.size(),limit)-pending.size());
    }
    private void upsert(JsonNode d, double[] embedding) {
        Set<String> required=new TreeSet<>(), all=new TreeSet<>();
        for (JsonNode t:d.path("terms")) if(t.path("type").asText().equals("ingredient")) {
            all.add(t.path("normalized").asText()); if (t.path("required").asBoolean()) required.add(t.path("normalized").asText());
        }
        jdbc.update("""
            INSERT INTO cocktail_search_document(cocktail_id,document,base_normalized,required_ingredients,all_ingredients,
              abv_min,abv_max,season,profile_text,profile_embedding,embedding_model,embedding_version,embedding_dimension,content_hash)
            VALUES (?,?::jsonb,ARRAY(SELECT jsonb_array_elements_text(?::jsonb)),ARRAY(SELECT jsonb_array_elements_text(?::jsonb)),
              ARRAY(SELECT jsonb_array_elements_text(?::jsonb)),?,?,?,?,?::vector,?,?,?,?)
            ON CONFLICT(cocktail_id) DO UPDATE SET document=excluded.document,base_normalized=excluded.base_normalized,
              required_ingredients=excluded.required_ingredients,all_ingredients=excluded.all_ingredients,abv_min=excluded.abv_min,
              abv_max=excluded.abv_max,season=excluded.season,profile_text=excluded.profile_text,profile_embedding=excluded.profile_embedding,
              embedding_model=excluded.embedding_model,embedding_version=excluded.embedding_version,embedding_dimension=excluded.embedding_dimension,
              content_hash=excluded.content_hash,updated_at=now()
            """,d.path("id").asLong(),d.toString(),d.path("bases").toString(),json(required),json(all),d.path("abv_min").asInt(),d.path("abv_max").asInt(),
                SearchDictionary.normalize(d.path("season").asText()),d.path("profile_text").asText(),json(embedding),provider.modelId(),provider.version(),provider.dimension(),d.path("content_hash").asText());
        jdbc.update("DELETE FROM cocktail_search_term WHERE cocktail_id=?",d.path("id").asLong());
        for (JsonNode t:d.path("terms")) term(d.path("id").asLong(),t.path("type").asText(),t.path("original").asText(),t.path("normalized").asText(),t.path("required").asBoolean(),t.path("source").asText());
        for (String field:List.of("taste_tags","aroma_tags","mood_tags")) for(JsonNode t:d.path(field)) term(d.path("id").asLong(),"tag",t.asText(),SearchDictionary.normalize(t.asText()),false,field);
    }
    private void term(long id,String type,String original,String normalized,boolean required,String source) {
        jdbc.update("""
            INSERT INTO cocktail_search_term VALUES (?,?,?,?,?,?) ON CONFLICT(cocktail_id,term_type,term_normalized)
            DO UPDATE SET is_required=cocktail_search_term.is_required OR excluded.is_required
            """,id,type,original,normalized,required,source);
    }
    private String json(Object value) { try {return mapper.writeValueAsString(value);} catch(Exception e){throw new IllegalArgumentException(e);} }
}
