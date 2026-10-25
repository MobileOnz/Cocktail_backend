package localharness;

import com.application.domain.cocktail.search.*;

import com.application.common.exception.CustomExceptionHandler;
import com.fasterxml.jackson.databind.*;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;

/** Local browser harness. Test classpath only; serves the real controller/service with a persistent CSV fixture. */
@Configuration(proxyBeanMethods=false)
@ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class,DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class,JacksonAutoConfiguration.class,HttpMessageConvertersAutoConfiguration.class})
@Import({HybridSearchController.class,HybridSearchService.class,OllamaEmbeddingProvider.class,CustomExceptionHandler.class})
public class HybridSearchLocalServer {
    private static DataSource localDataSource;
    public static void main(String[] args) throws Exception {
        String url=System.getenv().getOrDefault("SEARCH_LOCAL_JDBC_URL","jdbc:postgresql://127.0.0.1:55439/postgres");
        if (!url.matches("jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/[a-zA-Z0-9_]+"))
            throw new IllegalArgumentException("Browser harness accepts only an explicit loopback PostgreSQL database without URL options");
        String user=System.getenv().getOrDefault("SEARCH_LOCAL_DB_USER",System.getProperty("user.name"));
        String password=System.getenv("SEARCH_LOCAL_DB_PASSWORD");
        var admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));
        admin.execute("CREATE EXTENSION IF NOT EXISTS vector");
        admin.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        admin.execute("CREATE SCHEMA IF NOT EXISTS onz_homepage_demo");
        localDataSource=new DriverManagerDataSource(url+"?currentSchema=onz_homepage_demo,public",user,password);
        var jdbc=new JdbcTemplate(localDataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS onz_homepage_demo.cocktail(id bigint PRIMARY KEY,kor_name text NOT NULL,eng_name text NOT NULL,image_url text)");
        Flyway.configure().dataSource(localDataSource).schemas("onz_homepage_demo").locations("classpath:db/hybrid")
                .table("flyway_hybrid_search_history").baselineOnMigrate(true).baselineVersion("0").load().migrate();
        List<JsonNode> documents=new ArrayList<>();
        new ObjectMapper().readTree(Files.readString(Path.of("build/hybrid-search/documents.json"))).forEach(documents::add);
        for(JsonNode d:documents) jdbc.update("INSERT INTO onz_homepage_demo.cocktail VALUES(?,?,?,?) ON CONFLICT(id) DO NOTHING",
                d.path("id").asLong(),d.path("name_ko").asText(),d.path("name_en").asText(),d.path("image_url").asText());
        var provider=new OllamaEmbeddingProvider("http://127.0.0.1:11434","qwen3-embedding:0.6b","onz-profile-v1",
                "Instruct: Retrieve cocktails matching the requested ingredients, taste and aroma.\nQuery: ",120000);
        System.out.println(new SearchIndexer(jdbc,provider).index(documents,false,false,105,8));
        var app=new SpringApplication(HybridSearchLocalServer.class);
        app.setAdditionalProfiles("local");
        app.setDefaultProperties(Map.of("spring.config.name","hybrid-local","server.address","127.0.0.1",
                "server.port","8091","server.servlet.context-path","/onz","onz.search.enabled","true"));
        app.run(args);
    }
    @Bean DataSource dataSource(){return localDataSource;}
    @Bean NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource source){return new NamedParameterJdbcTemplate(source);}
}
