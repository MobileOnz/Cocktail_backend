package com.application.domain.cocktail.search;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class OllamaEmbeddingProviderTest {
    @Test void discoversDimensionAndRejectsChangedDimensionAndBadBatches() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var body=new java.util.concurrent.atomic.AtomicReference<>("{\"embeddings\":[[1,2,3]]}");
        server.createContext("/api/embed",exchange->{
            byte[] bytes=body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var p=new OllamaEmbeddingProvider("http://127.0.0.1:"+server.getAddress().getPort(),"model","v1","query: ",1000);
            assertThat(p.embedQuery("한글")).hasSize(3); assertThat(p.dimension()).isEqualTo(3);
            body.set("{\"embeddings\":[[1,2]]}"); assertThatThrownBy(()->p.embedQuery("x")).isInstanceOf(IllegalStateException.class);
            body.set("{\"embeddings\":[]}"); assertThatThrownBy(()->p.embedDocuments(List.of("x"))).isInstanceOf(IllegalStateException.class);
            body.set("{\"embeddings\":[[0,0,0]]}"); assertThatThrownBy(()->p.embedQuery("x")).isInstanceOf(IllegalStateException.class);
        } finally {server.stop(0);}
    }
    @Test void timeoutIsBounded() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/embed",exchange->{
            try {Thread.sleep(300);} catch(InterruptedException e){Thread.currentThread().interrupt();}
            exchange.close();
        });
        server.start();
        try {
            var p=new OllamaEmbeddingProvider("http://127.0.0.1:"+server.getAddress().getPort(),"model","v1","",50);
            assertThatThrownBy(()->p.embedQuery("x")).isInstanceOf(IllegalStateException.class).hasRootCauseInstanceOf(java.net.http.HttpTimeoutException.class);
        } finally {server.stop(0);}
    }
}
