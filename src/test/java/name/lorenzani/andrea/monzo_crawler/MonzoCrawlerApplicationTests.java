package name.lorenzani.andrea.monzo_crawler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole application, crawling a small real website over HTTP. */
@SpringBootTest(properties = {"crawler.poll-timeout=20ms", "crawler.request-timeout=2s"})
@AutoConfigureMockMvc
@Import(MonzoCrawlerApplicationTests.Reporting.class)
class MonzoCrawlerApplicationTests {

    @TestConfiguration
    static class Reporting {
        @Bean
        @Primary
        RecordingReporter recordingReporter() {
            return new RecordingReporter();
        }
    }

    private static HttpServer site;
    private static String base;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RecordingReporter reporter;

    private static void reply(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void html(String path, String links) {
        site.createContext(path, exchange -> reply(exchange, 200, "text/html",
                "<html><body>" + links + "</body></html>"));
    }

    @BeforeAll
    static void startSite() throws IOException {
        site = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        html("/", "<a href='/a'>a</a><a href='/b'>b</a><a href='https://example.org/outside'>out</a>"
                + "<a href='mailto:hello@example.org'>mail</a><a href='/missing'>missing</a>");
        html("/a", "<a href='/'>home</a><a href='/c#details'>c</a><a href='/handbook.pdf'>pdf</a>");
        html("/b", "<a href='/c'>c</a><a href='/b'>itself</a>");
        html("/c", "<p>the end</p>");
        site.createContext("/handbook.pdf", exchange -> reply(exchange, 200, "application/pdf", "%PDF-1.7"));
        site.createContext("/missing", exchange -> reply(exchange, 404, "text/html", "<html>nothing</html>"));
        site.start();
        base = "http://localhost:" + site.getAddress().getPort();
    }

    @AfterAll
    static void stopSite() {
        site.stop(0);
    }

    @Test
    void crawlsTheWebsiteAndShowsTheProgress() throws Exception {
        mvc.perform(post("/crawl").param("url", base + "/"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.host").value("localhost"));

        long deadline = System.nanoTime() + 20_000_000_000L;
        String state = "RUNNING";
        while (state.equals("RUNNING") && System.nanoTime() < deadline) {
            Thread.sleep(25);
            var response = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
            state = response.contains("\"state\":\"FINISHED\"") ? "FINISHED" : "RUNNING";
        }

        assertEquals("FINISHED", state);
        // 4 pages. /missing and /handbook.pdf are links that can't be visited: they are failures, not pages
        assertEquals(Set.of(base + "/", base + "/a", base + "/b", base + "/c"),
                new HashSet<>(reporter.visited()));
        assertEquals(List.of(base + "/a", base + "/b", "https://example.org/outside",
                "mailto:hello@example.org", base + "/missing"), reporter.linksOf(base + "/"));
        mvc.perform(get("/"))
                .andExpect(jsonPath("$.queuedLinks").value(0))
                .andExpect(jsonPath("$.crawls[0].pagesVisited").value(6))
                .andExpect(jsonPath("$.crawls[0].pagesFailed").value(2))
                .andExpect(jsonPath("$.crawls[0].pending").value(0))
                .andExpect(jsonPath("$.metrics.pageParseErrors").value(2));
    }

    @Test
    void refusesBadUrls() throws Exception {
        mvc.perform(post("/crawl").param("url", "ftp://example.org/")).andExpect(status().isBadRequest());
        mvc.perform(post("/crawl").param("url", "")).andExpect(status().isBadRequest());
    }

    @Test
    void doesNotExposeTheInternals() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }
}
