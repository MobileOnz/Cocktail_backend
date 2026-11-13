package com.application.domain.cocktail.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import java.util.*;
import static com.application.domain.cocktail.search.HybridSearchIntegrationTest.*;

/** Small, explicitly labelled local smoke set, not a human relevance benchmark. */
final class SearchEvaluation {
    record Case(String query, Set<Long> relevant, boolean name) {}
    private static Case c(String q,boolean name,Long...ids){return new Case(q,Set.of(ids),name);}
    static Map<String,Object> evaluate(HybridSearchService service,List<JsonNode> docs,JdbcTemplate jdbc,EmbeddingProvider provider) throws Exception {
        List<Case> cases=List.of(
            c("모히또",true,15L),c("모히토",true,15L),c("Mojito",true,15L),c("Piña Colada",true,18L),c("Pina Colada",true,18L),
            c("네그로니",true,70L),c("Espresso Martini",true,44L),c("화이트 럼 라임",false,5L,6L,15L,42L),
            c("진 베이스 상큼한 칵테일",false,8L,9L,25L,32L,46L,51L),c("진저 비어",false,16L,27L,96L),
            c("럼 없이 상큼한 칵테일",false,8L,9L,12L,28L,46L,51L),c("달지 않고 허브 향이 있는 칵테일",false,1L,65L,70L,87L),
            c("도수 10도 이하",false,1L,2L,14L,30L),c("진과 레몬이 들어간",false,8L,9L,25L,32L,46L),
            c("민트 라임",false,15L,48L,51L),c("커피",false,3L,43L,44L),c("코코넛 파인애플",false,18L),
            c("테킬라 베이스",false,12L,23L,28L,47L,53L),c("메스칼 베이스",false,57L,90L),
            c("카샤샤 베이스",false,35L,81L),c("계란 흰자가 들어간",false,58L,63L,73L,80L),
            c("레몬 주스",false,8L,9L,11L,22L,24L),c("보드카 베이스 커피",false,3L,44L),
            c("라임 없이",false,1L,2L,3L,4L),c("오렌지 탄산",false,1L,30L)
        );
        Map<Long,JsonNode> byId=new HashMap<>();docs.forEach(d->byId.put(d.path("id").asLong(),d));
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("evaluationScope","25 manually specified smoke queries; top5 hit=any labelled relevant ID, not independent human relevance assessment. Sweetness is not labelled/guaranteed.");
        report.put("model",provider.modelId()); report.put("dimension",provider.dimension());
        List<Object> modes=new ArrayList<>();
        for(String mode:List.of("keyword","vector","hybrid")) {
            service.search(request("모히또"),true,mode); // warm-up excluded
            List<Double> latency=new ArrayList<>(); List<Object> results=new ArrayList<>(); int hits=0,names=0,violations=0,degraded=0;
            for(Case c:cases) {
                long start=System.nanoTime(); var response=service.search(request(c.query),true,mode); latency.add((System.nanoTime()-start)/1e6);
                var items=items(response);
                boolean hit=items.stream().anyMatch(i->c.relevant.contains((Long)i.get("cocktailId")));
                if(hit)hits++;
                if(c.name&&!items.isEmpty()&&c.relevant.contains((Long)items.get(0).get("cocktailId")))names++;
                if(Boolean.TRUE.equals(response.get("degraded")))degraded++;
                var filters=(Map<?,?>)response.get("filters");
                for(var item:items) {
                    long id=(Long)item.get("cocktailId");
                    JsonNode d=byId.get(id);
                    Set<String> base=new HashSet<>();d.path("bases").forEach(b->base.add(b.asText()));
                    Set<String> required=new HashSet<>(),all=new HashSet<>();
                    for(JsonNode t:d.path("terms")) if(t.path("type").asText().equals("ingredient")){all.add(t.path("normalized").asText());if(t.path("required").asBoolean())required.add(t.path("normalized").asText());}
                    if(!base.containsAll((Collection<?>)filters.get("base"))||!Collections.disjoint(base,(Collection<?>)filters.get("excludedBase"))
                        ||!required.containsAll((Collection<?>)filters.get("requiredIngredients"))||!Collections.disjoint(all,(Collection<?>)filters.get("excludedIngredients"))
                        ||c.query.equals("도수 10도 이하")&&d.path("abv_max").asInt()>10)violations++;
                }
                results.add(Map.of("query",c.query,"relevantIds",c.relevant,"top5Hit",hit,"response",response));
            }
            List<Double> sorted=latency.stream().sorted().toList();
            modes.add(Map.of("mode",mode,"queries",cases.size(),"top5Hits",hits,"nameTop1Hits",names,"nameQueries",7,
                "filterViolations",violations,"degradedQueries",degraded,"p50Ms",sorted.get((int)Math.ceil(sorted.size()*.5)-1),"p95Ms",sorted.get((int)Math.ceil(sorted.size()*.95)-1),"results",results));
        }
        report.put("modes",modes);
        String vector=Arrays.toString(provider.embedQuery("상큼한 칵테일"));
        var exact=jdbc.queryForList("EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON) SELECT cocktail_id FROM cocktail_search_document ORDER BY profile_embedding <=> ?::vector LIMIT 5",vector);
        report.put("exactScanExplain",exact);
        int dimension=provider.dimension();
        if(dimension>0&&dimension<=2000) {
            // Only in the disposable test schema; dimension is validated structural SQL, not request data.
            jdbc.execute("CREATE INDEX evaluation_hnsw ON cocktail_search_document USING hnsw ((profile_embedding::vector("+dimension+")) vector_cosine_ops)");
            try {
                var tx=new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(jdbc.getDataSource())));
                Object explain=tx.execute(status->{
                    jdbc.execute("SET LOCAL enable_seqscan=off");
                    return jdbc.queryForList("EXPLAIN (ANALYZE,BUFFERS,FORMAT JSON) SELECT cocktail_id FROM cocktail_search_document ORDER BY profile_embedding::vector("+dimension+") <=> ?::vector LIMIT 5",vector);
                });
                report.put("hnswExplain",explain);
            } finally {jdbc.execute("DROP INDEX evaluation_hnsw");}
        }
        return report;
    }
}
