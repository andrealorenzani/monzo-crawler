package name.lorenzani.andrea.monzo_crawler.fetch;

import com.sun.net.httpserver.HttpServer;
import name.lorenzani.andrea.monzo_crawler.config.CrawlerProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsoupPageFetcherTest {

    private static HttpServer server;
    private static String base;
    private static final AtomicReference<String> userAgent = new AtomicReference<>();

    private final JsoupPageFetcher fetcher = new JsoupPageFetcher(new LinkExtractor(),
            new CrawlerProperties(0, 10, Duration.ofSeconds(2), Duration.ofMillis(10), "test-crawler"));

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/page", exchange -> {
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            reply(exchange, 200, "text/html; charset=utf-8",
                    "<html><body><a href=\"/other\">1</a><a href=\"relative\">2</a><a href=\"http://external.test/x#y\">3</a></body></html>");
        });
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().set("Location", "/page");
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });
        server.createContext("/document.pdf", exchange -> reply(exchange, 200, "application/pdf", "%PDF-1.7"));
        server.createContext("/gone", exchange -> reply(exchange, 404, "text/html", "<html>not here</html>"));
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsThePageAndExtractsTheLinks() throws IOException {
        Page page = fetcher.fetch(base + "/page");

        assertEquals(base + "/page", page.finalUrl());
        assertEquals(List.of(base + "/other", base + "/relative", "http://external.test/x#y"), page.links());
    }

    @Test
    void introducesItself() throws IOException {
        fetcher.fetch(base + "/page");

        assertEquals("test-crawler", userAgent.get());
    }

    @Test
    void reportsWhereRedirectsLead() throws IOException {
        Page page = fetcher.fetch(base + "/moved");

        assertEquals(base + "/page", page.finalUrl());
        assertEquals(3, page.links().size());
    }

    @Test
    void failsOnPagesThatAreNotHtml() {
        assertThrows(IOException.class, () -> fetcher.fetch(base + "/document.pdf"));
    }

    @Test
    void failsOnHttpErrors() {
        assertThrows(IOException.class, () -> fetcher.fetch(base + "/gone"));
    }

    @Test
    void failsWhenNobodyListens() {
        assertThrows(IOException.class, () -> fetcher.fetch("http://localhost:1/"));
    }
}
