package com.application.domain.cocktail.search;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class HybridSearchService {
    private static final Logger LOG=LoggerFactory.getLogger(HybridSearchService.class);
    private final NamedParameterJdbcTemplate jdbc;
    private final EmbeddingProvider embedder;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final String sql;
    private final double nameWeight, ingredientWeight, flavorWeight, mixedWeight;
    private final int candidates, rrfK;
    public HybridSearchService(NamedParameterJdbcTemplate jdbc, EmbeddingProvider embedder, ObjectMapper mapper,
            @Value("${onz.search.enabled:false}") boolean enabled,
            @Value("${onz.search.rrf.name-keyword-weight:0.85}") double nameWeight,
            @Value("${onz.search.rrf.ingredient-keyword-weight:0.75}") double ingredientWeight,
            @Value("${onz.search.rrf.flavor-keyword-weight:0.25}") double flavorWeight,
            @Value("${onz.search.rrf.mixed-keyword-weight:0.5}") double mixedWeight,
            @Value("${onz.search.candidate-limit:50}") int candidates,
            @Value("${onz.search.rrf.k:60}") int rrfK) throws java.io.IOException {
        this.jdbc=jdbc; this.embedder=embedder; this.mapper=mapper; this.enabled=enabled;
        this.nameWeight=nameWeight; this.ingredientWeight=ingredientWeight; this.flavorWeight=flavorWeight; this.mixedWeight=mixedWeight;
        this.candidates=candidates; this.rrfK=rrfK;
        if (candidates<1 || candidates>200 || rrfK<1 || java.util.stream.DoubleStream.of(nameWeight,ingredientWeight,flavorWeight,mixedWeight).anyMatch(w->!Double.isFinite(w)||w<0||w>1))
            throw new IllegalArgumentException("Invalid search ranking configuration");
        sql=new ClassPathResource("search/hybrid.sql").getContentAsString(StandardCharsets.UTF_8);
    }
    public Map<String,Object> search(HybridSearchRequest r, boolean debugAllowed) { return search(r,debugAllowed,"hybrid"); }
    // Evaluation uses exactly the production retrieval path; mode is not exposed by the public API.
    public Map<String,Object> search(HybridSearchRequest r, boolean debugAllowed, String mode) {
        validate(r);
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Hybrid search is not enabled");
        try { return execute(r,debugAllowed,mode); }
        catch (ResponseStatusException e) { throw e; }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); }
        catch (Exception e) {
            LOG.error("Hybrid search DB/index failure",e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Search index unavailable");
        }
    }
    private Map<String,Object> execute(HybridSearchRequest r, boolean debugAllowed, String mode) throws Exception {
        if (!Set.of("hybrid","keyword","vector").contains(mode)) throw new IllegalArgumentException("Invalid search mode");
        if (Boolean.TRUE.equals(r.debug()) && !debugAllowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Search debug is development-only");
        // ponytail: full entity vocabulary per query suits 105 documents; cache/version it when the catalogue grows.
        Set<String> entities=new HashSet<>(), bases=new HashSet<>();
        jdbc.query("SELECT DISTINCT term_type,term_normalized FROM cocktail_search_term WHERE term_type IN ('ingredient','base')",Map.of(),rs->{
            if (rs.getString(1).equals("base")) bases.add(rs.getString(2)); else entities.add(rs.getString(2));
        });
        if (entities.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Search index is empty");
        SearchQuery q=SearchQuery.analyze(r,entities,bases);
        List<String> warnings=new ArrayList<>();
        double[] vector=null;
        if (!mode.equals("keyword")) {
            try { vector=embedder.embedQuery(q.text()); EmbeddingProvider.validate(vector,embedder.dimension()); }
            catch (RuntimeException e) {
                vector=null;
                LOG.warn("Embedding unavailable; keyword fallback: {}",e.getClass().getSimpleName());
                warnings.add("EMBEDDING_UNAVAILABLE_KEYWORD_FALLBACK");
            }
        }
        boolean useVector=vector!=null;
        if (useVector) {
            Integer incompatible=jdbc.queryForObject("SELECT count(*) FROM cocktail_search_document WHERE embedding_model<>:model OR embedding_version<>:version OR embedding_dimension<>:dimension",
                    Map.of("model",embedder.modelId(),"version",embedder.version(),"dimension",embedder.dimension()),Integer.class);
            if (incompatible!=null && incompatible>0) {
                // Never silently mix partially reindexed model versions. Keyword channel stays usable.
                useVector=false; warnings.add("INDEX_MODEL_MISMATCH_KEYWORD_FALLBACK");
            }
        }
        boolean exactName=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM cocktail_search_term WHERE term_type IN ('name','alias') AND term_normalized=:q)",Map.of("q",q.text()),Boolean.class));
        boolean ingredients=q.terms().stream().anyMatch(entities::contains);
        String type=exactName?"name":ingredients?"ingredient":"flavor";
        if (ingredients && q.text().matches(".*(?:상큼|향|달콤|청량).*")) type="mixed";
        double weight=switch(type) {case "name"->nameWeight; case "ingredient"->ingredientWeight; case "mixed"->mixedWeight; default->flavorWeight;};
        boolean useKeyword=!mode.equals("vector") || !useVector;
        var p=new MapSqlParameterSource().addValue("raw",r.query().trim()).addValue("query",q.text())
                .addValue("bases",json(q.bases())).addValue("excludedBases",json(q.excludedBases()))
                .addValue("required",json(q.required())).addValue("excluded",json(q.excluded()))
                .addValue("terms",json(q.terms())).addValue("abvMin",q.abvMin()).addValue("abvMax",q.abvMax()).addValue("season",q.season())
                .addValue("nameSearch",exactName||!ingredients).addValue("filterOnly",q.filterOnly()).addValue("embedding",vector==null?null:json(vector))
                .addValue("model",embedder.modelId()).addValue("version",embedder.version()).addValue("dimension",embedder.dimension())
                .addValue("useKeyword",useKeyword).addValue("useVector",useVector).addValue("candidates",candidates)
                .addValue("rrfK",rrfK).addValue("keywordWeight",mode.equals("keyword")||!useVector?1:weight)
                .addValue("vectorWeight",mode.equals("vector")?1:1-weight);
        List<Map<String,Object>> rows=jdbc.query(sql,p,(rs,n)->{
            JsonNode doc=readDocument(rs.getString("document"));
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("cocktailId",doc.path("id").asLong()); item.put("nameKo",doc.path("name_ko").asText()); item.put("nameEn",doc.path("name_en").asText());
            for (String field:List.of("base_raw","bases","taste_tags","aroma_tags","ingredients_raw","ingredients","image_url","abv_min","abv_max")) item.put(field,doc.path(field));
            item.put("image_url",rs.getString("current_image_url"));
            Set<String> reasons=new LinkedHashSet<>();
            for (JsonNode term:doc.path("terms")) {
                String normalized=term.path("normalized").asText();
                if (normalized.equals(q.text()) || q.terms().contains(normalized)) reasons.add(term.path("source").asText()+": "+term.path("original").asText());
            }
            for (JsonNode tag:doc.path("taste_tags")) if (q.terms().contains(SearchDictionary.normalize(tag.asText()))) reasons.add("맛: "+tag.asText());
            if (reasons.isEmpty() && rs.getObject("vector_rank")!=null) reasons.add("원본 프로필과 의미 유사 (재료 포함·단맛을 보장하지 않음)");
            item.put("matchReasons",reasons);
            if (Boolean.TRUE.equals(r.debug()) && debugAllowed) {
                Map<String,Object> debug=new LinkedHashMap<>(); debug.put("keywordRank",rs.getObject("keyword_rank")); debug.put("vectorRank",rs.getObject("vector_rank")); debug.put("rrfScore",rs.getDouble("rrf_score")); item.put("debug",debug);
            }
            return item;
        });
        int offset=r.offset()==null?0:r.offset(), limit=r.limit()==null?10:r.limit();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("items",rows.subList(Math.min(offset,rows.size()),Math.min(offset+limit,rows.size())));
        result.put("searchType",type); result.put("mode",useVector?(useKeyword?"hybrid":"vector"):"keyword"); result.put("degraded",!warnings.isEmpty());
        result.put("warnings",warnings); result.put("offset",offset); result.put("limit",limit); result.put("totalCandidates",rows.size()); result.put("hasMore",offset+limit<rows.size());
        result.put("abvPolicy","abv_min >= requested minimum AND abv_max <= requested maximum; entire source range must fit");
        result.put("sweetnessPolicy","맛단계 없음: 단맛 수치 필터 및 무가당 보장 없음");
        result.put("filters",Map.of("base",q.bases(),"excludedBase",q.excludedBases(),"requiredIngredients",q.required(),"excludedIngredients",q.excluded()));
        return result;
    }
    private JsonNode readDocument(String text) { try { return mapper.readTree(text); } catch(Exception e) { throw new IllegalStateException("Invalid indexed document",e); } }
    private String json(Object object) { try { return mapper.writeValueAsString(object); } catch(Exception e) { throw new IllegalArgumentException(e); } }
    private static void validate(HybridSearchRequest r) {
        if (r.query()==null || r.query().isBlank() || r.query().length()>300 || r.query().chars().anyMatch(Character::isISOControl)) bad("query must contain 1~300 printable characters");
        if (r.limit()!=null&&(r.limit()<1||r.limit()>100) || r.offset()!=null&&(r.offset()<0||r.offset()>10000)) bad("Invalid pagination");
        if (r.abvMin()!=null&&(r.abvMin()<0||r.abvMin()>100) || r.abvMax()!=null&&(r.abvMax()<0||r.abvMax()>100) || r.abvMin()!=null&&r.abvMax()!=null&&r.abvMin()>r.abvMax()) bad("Invalid ABV range");
        for (var values:Arrays.asList(r.base(),r.excludedBase(),r.requiredIngredients(),r.excludedIngredients())) if (values!=null&&(values.size()>20||values.stream().anyMatch(v->v==null||v.isBlank()||v.length()>100))) bad("Invalid filter");
        if (r.season()!=null&&(r.season().isBlank()||r.season().length()>30)) bad("Invalid season");
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
