package com.application.domain.cocktail.search;

import com.fasterxml.jackson.databind.*;
import com.application.common.exception.CustomExceptionHandler;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL/pgvector; unique schema, no application/production tables changed. */
@EnabledIfEnvironmentVariable(named="SEARCH_TEST_JDBC_URL",matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HybridSearchIntegrationTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private JdbcTemplate jdbc, admin;
    private String schema;
    private List<JsonNode> docs;
    private HybridSearchService service;
    private final EmbeddingProvider mock=new MockEmbedding();
    @BeforeAll void prepare() throws Exception {
        String url=System.getenv("SEARCH_TEST_JDBC_URL");
        admin=new JdbcTemplate(new DriverManagerDataSource(url,System.getenv("SEARCH_TEST_DB_USER"),System.getenv("SEARCH_TEST_DB_PASSWORD")));
        admin.execute("CREATE EXTENSION IF NOT EXISTS vector"); admin.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        schema="onz_search_test_"+UUID.randomUUID().toString().replace("-","");
        admin.execute("CREATE SCHEMA "+schema);
        var ds=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema+",public",System.getenv("SEARCH_TEST_DB_USER"),System.getenv("SEARCH_TEST_DB_PASSWORD"));
        jdbc=new JdbcTemplate(ds);
        // Only columns touched by this additive feature. IDs/names come from the real CSV fixture.
        jdbc.execute("CREATE TABLE cocktail(id bigint PRIMARY KEY,kor_name text NOT NULL,eng_name text NOT NULL,image_url text)");
        Flyway.configure().dataSource(ds).schemas(schema).locations("classpath:db/hybrid").table("flyway_hybrid_search_history").baselineOnMigrate(true).baselineVersion("0").load().migrate();
        docs=new ArrayList<>(); mapper.readTree(Files.readString(Path.of("build/hybrid-search/documents.json"))).forEach(docs::add);
        for(var d:docs) jdbc.update("INSERT INTO cocktail VALUES(?,?,?,?)",d.path("id").asLong(),d.path("name_ko").asText(),d.path("name_en").asText(),d.path("image_url").asText());
        assertThat(new SearchIndexer(jdbc,mock).index(docs,false,false,105,8).get("indexed")).isEqualTo(105);
        service=service(mock);
    }
    @AfterAll void cleanup() { if(admin!=null&&schema!=null) admin.execute("DROP SCHEMA "+schema+" CASCADE"); }
    private HybridSearchService service(EmbeddingProvider p) throws Exception {
        return new HybridSearchService(new NamedParameterJdbcTemplate(jdbc),p,mapper,true,.85,.75,.25,.5,50,60);
    }
    static HybridSearchRequest request(String q) { return new HybridSearchRequest(q,null,null,null,null,null,null,null,5,0,true); }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> items(Map<String,Object> response) { return (List<Map<String,Object>>)response.get("items"); }
    @Test void sourceAndIdempotentIndex() {
        assertThat(docs).hasSize(105);
        assertThat(new SearchIndexer(jdbc,mock).index(docs,false,false,105,8).get("indexed")).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cocktail_search_document",Integer.class)).isEqualTo(105);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cocktail",Integer.class)).isEqualTo(105);
        assertThat(new SearchIndexer(jdbc,mock).index(docs,true,true,2,1).get("indexed")).isZero();
        assertThat(jdbc.queryForObject("SELECT vector_dims(profile_embedding) FROM cocktail_search_document LIMIT 1",Integer.class)).isEqualTo(64);
    }
    @Test void changedContentOnlyIsReembedded() {
        var changed=docs.get(0).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode)changed).put("content_hash","changed-for-regression");
        ((com.fasterxml.jackson.databind.node.ObjectNode)changed).put("profile_text","changed profile");
        assertThat(new SearchIndexer(jdbc,mock).index(List.of(changed),false,false,1,1).get("indexed")).isEqualTo(1);
        assertThat(new SearchIndexer(jdbc,mock).index(docs,false,false,105,8).get("indexed")).isEqualTo(1);
    }
    @Test void namesArePinnedEvenAgainstVectorCandidates() {
        for(String q:List.of("모히또","모히토","Mojito","ＭＯＪＩＴＯ")) assertThat(items(service.search(request(q),true)).get(0).get("cocktailId")).isEqualTo(15L);
        assertThat(items(service.search(request("진 피"),true)).get(0).get("cocktailId")).isEqualTo(8L);
        for(String q:List.of("Piña Colada","Pina Colada","피나 콜라다")) assertThat(items(service.search(request(q),true)).get(0).get("cocktailId")).isEqualTo(18L);
    }
    @Test void strictFiltersCannotBeResurrectedByVectorAndGingerIsNotGin() {
        for(var item:items(service.search(request("진 베이스 상큼한 칵테일"),true))) assertThat(item.get("bases").toString()).contains("\"진\"");
        for(var item:items(service.search(request("럼 없이 상큼한 칵테일"),true))) assertThat(item.get("bases").toString()).doesNotContain("\"럼\"");
        for(var item:items(service.search(request("진과 레몬이 들어간"),true))) {
            var actual=jdbc.queryForObject("SELECT required_ingredients @> ARRAY['진','레몬'] FROM cocktail_search_document WHERE cocktail_id=?",Boolean.class,item.get("cocktailId"));
            assertThat(actual).isTrue();
        }
        var empty=new HybridSearchRequest("상큼",null,null,List.of("존재하지않는재료"),null,null,null,null,100,0,true);
        assertThat(items(service.search(empty,true))).isEmpty();
        var rare=new HybridSearchRequest("상큼",null,null,List.of("화이트 그라파"),null,null,null,null,100,0,true);
        assertThat(items(service.search(rare,true))).extracting(i->i.get("cocktailId")).containsExactly(99L);
        assertThat(items(service.search(request("메스칼 베이스"),true))).extracting(i->i.get("cocktailId")).containsExactlyInAnyOrder(57L,90L);
        assertThat(items(service.search(request("진과 레몬이 들어간"),true))).isNotEmpty();
        assertThat(items(service.search(request("도수 10도 이하"),true))).isNotEmpty();
        var ginger=service.search(request("진저 비어"),true);
        assertThat(ginger.get("filters").toString()).doesNotContain("requiredIngredients=[진]");
        var gin=SearchQuery.analyze(request("진저 비어"),Set.of("진","진저 비어"),Set.of("진"));
        assertThat(gin.terms()).doesNotContain("진");
        assertThat(jdbc.queryForObject("SELECT required_ingredients @> ARRAY['계란 흰자'] FROM cocktail_search_document WHERE cocktail_id=24",Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT required_ingredients @> ARRAY['버번 위스키'] FROM cocktail_search_document WHERE cocktail_id=71",Boolean.class)).isFalse();
    }
    @Test void abvAndPaginationAndRrf() {
        for(var item:items(service.search(request("도수 10도 이하"),true))) assertThat(((JsonNode)item.get("abv_max")).asInt()).isLessThanOrEqualTo(10);
        var all=service.search(new HybridSearchRequest("상큼",null,null,null,null,null,null,null,100,0,true),true);
        var page=service.search(new HybridSearchRequest("상큼",null,null,null,null,null,null,null,5,5,true),true);
        assertThat(items(page)).isEqualTo(items(all).subList(5,10));
        for(var item:items(all)) {
            var debug=(Map<?,?>)item.get("debug");
            double expected=(debug.get("keywordRank")==null?0:.25/(60+((Number)debug.get("keywordRank")).doubleValue()))
                    +(debug.get("vectorRank")==null?0:.75/(60+((Number)debug.get("vectorRank")).doubleValue()));
            assertThat((Double)debug.get("rrfScore")).isCloseTo(expected,within(1e-12));
        }
    }
    @Test void missingChannelContributesZeroAndFallbackIsVisible() throws Exception {
        var broken=new MockEmbedding(){@Override public double[] embedQuery(String q){throw new IllegalStateException("timeout");}};
        var response=service(broken).search(request("모히토"),true);
        assertThat(response.get("mode")).isEqualTo("keyword"); assertThat(response.get("degraded")).isEqualTo(true);
        assertThat(items(response).get(0).get("cocktailId")).isEqualTo(15L);
        assertThat(((Map<?,?>)items(response).get(0).get("debug")).get("vectorRank")).isNull();
        var mismatch=new MockEmbedding(){@Override public String version(){return "other-version";}};
        assertThat(service(mismatch).search(request("모히토"),true).get("warnings").toString()).contains("INDEX_MODEL_MISMATCH");
        var wrongDimension=new MockEmbedding(){@Override public double[] embedQuery(String q){return new double[]{1,2};}};
        assertThat(service(wrongDimension).search(request("모히토"),true).get("degraded")).isEqualTo(true);
    }
    @Test void publicControllerValidationAndProductionDebug() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new HybridSearchController(service,new MockEnvironment().withProperty("spring.profiles.active","prod")))
                .setControllerAdvice(new CustomExceptionHandler()).build();
        mvc.perform(get("/api/v2/cocktails/search/hybrid").param("query","모히토")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].cocktailId").value(15)).andExpect(jsonPath("$.data.items[0].debug").doesNotExist());
        mvc.perform(get("/api/v2/cocktails/search/hybrid").param("query"," ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v2/cocktails/search/hybrid").param("query","모히토").param("limit","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v2/cocktails/search/hybrid").param("query","모히토").param("debug","true")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v2/cocktails/search/hybrid").param("query","' OR 1=1; --")).andExpect(status().isOk());
    }
    @Test void modelAndBatchErrorsCannotOverwriteStoredRows() {
        var changed=docs.get(0).deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode)changed).put("name_ko","wrong identity");
        assertThatThrownBy(()->new SearchIndexer(jdbc,mock).index(List.of(changed),false,true,1,1)).hasMessageContaining("identity mismatch");
        var invalid=new MockEmbedding(){@Override public List<double[]> embedDocuments(List<String> q){return List.of(new double[]{Double.NaN});}};
        assertThatThrownBy(()->new SearchIndexer(jdbc,invalid).index(docs,false,true,1,1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cocktail_search_document",Integer.class)).isEqualTo(105);
    }
    @Test @EnabledIfEnvironmentVariable(named="SEARCH_LIVE_EMBEDDING",matches="1")
    void realLocalModelEvaluation() throws Exception {
        var local=new OllamaEmbeddingProvider("http://127.0.0.1:11434","qwen3-embedding:0.6b","onz-profile-v1","Instruct: Retrieve cocktails matching the requested ingredients, taste and aroma.\nQuery: ",120000);
        try {
            var indexed=new SearchIndexer(jdbc,local).index(docs,false,true,105,8);
            HybridSearchService liveService=service(local);
            var mvc=MockMvcBuilders.standaloneSetup(new HybridSearchController(liveService,new MockEnvironment()))
                    .setControllerAdvice(new CustomExceptionHandler()).build();
            String apiSample=mvc.perform(get("/api/v2/cocktails/search/hybrid").param("query","모히토"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].cocktailId").value(15))
                    .andExpect(jsonPath("$.data.mode").value("hybrid")).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            var report=SearchEvaluation.evaluate(liveService,docs,jdbc,local);
            report.put("apiSample",mapper.readTree(apiSample));
            report.put("indexing",indexed);
            Files.createDirectories(Path.of("build/hybrid-search"));
            mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of("build/hybrid-search/evaluation.json").toFile(),report);
        } finally { new SearchIndexer(jdbc,mock).index(docs,false,true,105,8); }
    }
    static class MockEmbedding implements EmbeddingProvider {
        public double[] embedQuery(String text){return vector(text);}
        public List<double[]> embedDocuments(List<String> texts){return texts.stream().map(this::vector).toList();}
        private double[] vector(String s){double[] v=new double[64]; for(int i=0;i<s.length();i++)v[Math.floorMod(s.charAt(i)*31,64)]+=1; if(s.isEmpty())v[0]=1;return v;}
        public int dimension(){return 64;}
        public String modelId(){return "deterministic-test-only";}
        public String version(){return "test-v1";}
    }
}
