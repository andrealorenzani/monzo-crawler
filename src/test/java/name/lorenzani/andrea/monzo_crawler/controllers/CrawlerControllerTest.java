package name.lorenzani.andrea.monzo_crawler.controllers;

import name.lorenzani.andrea.monzo_crawler.crawl.CrawlSession;
import name.lorenzani.andrea.monzo_crawler.crawl.CrawlerService;
import name.lorenzani.andrea.monzo_crawler.metrics.MetricService;
import name.lorenzani.andrea.monzo_crawler.queue.CrawlQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrawlerController.class)
class CrawlerControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CrawlerService crawler;

    @MockitoBean
    private CrawlQueue queue;

    @MockitoBean
    private MetricService metrics;

    @Test
    void startsACrawlAndAnswers202() throws Exception {
        var session = newSession();
        when(crawler.start("http://monzo.com")).thenReturn(new CrawlerService.StartResult(CrawlerService.Start.STARTED, session));

        mvc.perform(post("/crawl").param("url", "http://monzo.com"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Crawl", "STARTED"))
                .andExpect(jsonPath("$.host").value("monzo.com"))
                .andExpect(jsonPath("$.state").value("RUNNING"))
                .andExpect(jsonPath("$.startUrl").value("http://monzo.com/"));
    }

    @Test
    void saysWhenTheCrawlIsAlreadyRunning() throws Exception {
        when(crawler.start("http://monzo.com")).thenReturn(
                new CrawlerService.StartResult(CrawlerService.Start.ALREADY_RUNNING, newSession()));

        mvc.perform(post("/crawl").param("url", "http://monzo.com"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Crawl", "ALREADY_RUNNING"));
    }

    @Test
    void anInvalidUrlIsABadRequest() throws Exception {
        when(crawler.start("not a url")).thenThrow(new IllegalArgumentException("Not an absolute http or https URL: not a url"));

        mvc.perform(post("/crawl").param("url", "not a url"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not an absolute http or https URL: not a url"));
    }

    @Test
    void theUrlIsMandatory() throws Exception {
        mvc.perform(post("/crawl")).andExpect(status().isBadRequest());
    }

    @Test
    void crawlOnlyAcceptsPost() throws Exception {
        mvc.perform(get("/crawl").param("url", "http://monzo.com")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void showsWhatTheCrawlerIsDoing() throws Exception {
        when(crawler.sessions()).thenReturn(List.of(newSession()));
        when(queue.size()).thenReturn(7);
        when(metrics.getMetrics()).thenReturn(Map.of("linkVisited", 3L));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queuedLinks").value(7))
                .andExpect(jsonPath("$.crawls[0].host").value("monzo.com"))
                .andExpect(jsonPath("$.crawls[0].pagesVisited").value(0))
                .andExpect(jsonPath("$.metrics.linkVisited").value(3));
    }

    private static CrawlSession newSession() {
        return new CrawlSession("monzo.com", "http://monzo.com/", Instant.parse("2026-01-01T00:00:00Z"), 100);
    }
}
