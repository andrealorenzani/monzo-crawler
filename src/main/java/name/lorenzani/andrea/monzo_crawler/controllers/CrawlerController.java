package name.lorenzani.andrea.monzo_crawler.controllers;

import name.lorenzani.andrea.monzo_crawler.crawl.CrawlSession;
import name.lorenzani.andrea.monzo_crawler.crawl.CrawlerService;
import name.lorenzani.andrea.monzo_crawler.metrics.MetricService;
import name.lorenzani.andrea.monzo_crawler.queue.CrawlQueue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
public class CrawlerController {

    public record CrawlStatus(String host, String startUrl, CrawlSession.State state, Instant startedAt,
                              int pagesVisited, int pagesFailed, int pending, boolean truncated) {
        static CrawlStatus of(CrawlSession session) {
            return new CrawlStatus(session.host(), session.startUrl(), session.state(), session.startedAt(),
                    session.pagesVisited(), session.pagesFailed(), session.pending(), session.truncated());
        }
    }

    public record Status(int queuedLinks, List<CrawlStatus> crawls, Map<String, Long> metrics) {
    }

    private final CrawlerService crawler;
    private final CrawlQueue queue;
    private final MetricService metrics;

    public CrawlerController(CrawlerService crawler, CrawlQueue queue, MetricService metrics) {
        this.crawler = crawler;
        this.queue = queue;
        this.metrics = metrics;
    }

    /** What the crawler is doing: the crawls, with the number of pages they visited, and the counters. */
    @GetMapping("/")
    public Status status() {
        List<CrawlStatus> crawls = crawler.sessions().stream()
                .map(CrawlStatus::of)
                .sorted(Comparator.comparing(CrawlStatus::host))
                .toList();
        return new Status(queue.size(), crawls, metrics.getMetrics());
    }

    /**
     * Starts crawling the host of the URL. The result is printed by the crawler; follow the progress with {@code GET /}.
     * Answers 202 when the crawl starts, or when the host is being crawled already (the crawl just continues).
     */
    @PostMapping("/crawl")
    public ResponseEntity<CrawlStatus> crawl(@RequestParam("url") String url) {
        var result = crawler.start(url);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("X-Crawl", result.outcome().name())
                .body(CrawlStatus.of(result.session()));
    }
}
