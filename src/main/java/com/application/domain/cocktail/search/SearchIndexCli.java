package com.application.domain.cocktail.search;

import com.fasterxml.jackson.databind.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.file.*;
import java.util.*;

public final class SearchIndexCli {
    private SearchIndexCli() {}
    public static void main(String[] args) throws Exception {
        Set<String> flags=Set.of("--dry-run","--force-reembed");
        Set<String> values=Set.of("--input","--limit","--batch-size");
        if(args.length==0||!Set.of("migrate","index").contains(args[0])) throw new IllegalArgumentException("migrate | index --input documents.json [--dry-run] [--force-reembed] [--limit N] [--batch-size N]");
        Map<String,String> options=new HashMap<>();
        for(int i=1;i<args.length;i++) {
            if(flags.contains(args[i])) options.put(args[i],"true");
            else if(values.contains(args[i])&&i+1<args.length) options.put(args[i],args[++i]);
            else throw new IllegalArgumentException("Unknown/missing argument: "+args[i]);
        }
        String url=System.getenv("SEARCH_JDBC_URL");
        if(url==null) throw new IllegalArgumentException("Set SEARCH_JDBC_URL explicitly; no default production connection");
        var ds=new DriverManagerDataSource(url,System.getenv("SEARCH_DB_USER"),System.getenv("SEARCH_DB_PASSWORD"));
        if(args[0].equals("migrate")) {
            Flyway.configure().dataSource(ds).locations("classpath:db/hybrid").table("flyway_hybrid_search_history")
                    .baselineOnMigrate(true).baselineVersion("0").load().migrate(); return;
        }
        String input=options.get("--input");
        if(input==null) throw new IllegalArgumentException("--input required");
        var mapper=new ObjectMapper();
        List<JsonNode> docs=new ArrayList<>(); mapper.readTree(Files.readString(Path.of(input))).forEach(docs::add);
        var provider=new OllamaEmbeddingProvider(env("SEARCH_EMBEDDING_URL","http://127.0.0.1:11434"),env("SEARCH_EMBEDDING_MODEL","qwen3-embedding:0.6b"),
                env("SEARCH_EMBEDDING_VERSION","onz-profile-v1"),env("SEARCH_QUERY_PREFIX","Instruct: Retrieve cocktails matching the requested ingredients, taste and aroma.\nQuery: "),Long.parseLong(env("SEARCH_EMBEDDING_TIMEOUT_MS","120000")));
        try {
            var result=new SearchIndexer(new JdbcTemplate(ds),provider).index(docs,options.containsKey("--dry-run"),options.containsKey("--force-reembed"),
                    Integer.parseInt(options.getOrDefault("--limit","2147483647")),Integer.parseInt(options.getOrDefault("--batch-size","8")));
            System.out.println(mapper.writeValueAsString(result));
        } catch (RuntimeException e) { System.err.println("Index failed; completed batches remain resumable: "+e.getMessage()); throw e; }
    }
    private static String env(String key,String fallback) { return System.getenv().getOrDefault(key,fallback); }
}
