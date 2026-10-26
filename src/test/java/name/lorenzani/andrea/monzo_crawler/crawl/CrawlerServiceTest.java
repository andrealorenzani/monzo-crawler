package name.lorenzani.andrea.monzo_crawler.crawl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import name.lorenzani.andrea.monzo_crawler.FakeSite;
import name.lorenzani.andrea.monzo_crawler.RecordingReporter;
import name.lorenzani.andrea.monzo_crawler.config.CrawlerProperties;
import name.lorenzani.andrea.monzo_crawler.metrics.MetricService;
import name.lorenzani.andrea.monzo_crawler.queue.InMemoryCrawlQueue;
import name.lorenzani.andrea.monzo_crawler.visited.InMemoryVisitedStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrawlerServiceTest {

    private FakeSite site;
    private InMemoryCrawlQueue queue;
    private InMemoryVisitedStore visited;
    private RecordingReporter reporter;
    private MetricService metrics;

    @BeforeEach
    void setUp() {
        site = new FakeSite();
        queue = new InMemoryCrawlQueue();
        visited = new InMemoryVisitedStore();
        reporter = new RecordingReporter();
        metrics = new MetricService(new SimpleMeterRegistry());
    }

    private CrawlerService service(int maxPages) {
        var properties = new CrawlerProperties(0, maxPages, Duration.ofSeconds(1), Duration.ofMillis(10), "test");
        return new CrawlerService(queue, visited, site, reporter, metrics, properties,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    /** Does the work of the workers, in this thread. */
    private void drain(CrawlerService service) throws InterruptedException {
        for (var next = queue.receive(Duration.ZERO); next.isPresent(); next = queue.receive(Duration.ZERO)) {
            service.process(next.get());
        }
    }

    private CrawlSession crawl(CrawlerService service, String url) throws InterruptedException {
        var session = service.start(url).session();
        drain(service);
        return session;
    }

    @Test
    void visitsEveryPageOfTheSiteOnceAndReportsAllTheLinks() throws Exception {
        site.page("http://monzo.com/", "http://monzo.com/a", "http://monzo.com/b", "https://twitter.com/monzo", "mailto:hi@monzo.com")
                .page("http://monzo.com/a", "http://monzo.com/", "http://monzo.com/b", "http://monzo.com/a#section", "http://monzo.com/c")
                .page("http://monzo.com/b", "http://monzo.com/c")
                .page("http://monzo.com/c");

        var session = crawl(service(100), "http://monzo.com/");

        assertEquals(List.of("http://monzo.com/", "http://monzo.com/a", "http://monzo.com/b", "http://monzo.com/c"),
                reporter.visited());
        site.allDownloads().keySet().forEach(url -> assertEquals(1, site.downloads(url), url));
        // the links of a page are reported all, as the page has them, wherever they point to
        assertEquals(List.of("http://monzo.com/a", "http://monzo.com/b", "https://twitter.com/monzo", "mailto:hi@monzo.com"),
                reporter.linksOf("http://monzo.com/"));
        assertEquals(CrawlSession.State.FINISHED, session.state());
        assertEquals(4, session.pagesVisited());
        assertEquals(0, session.pending());
        assertFalse(session.truncated());
    }

    @Test
    void doesNotLeaveTheHost() throws Exception {
        site.page("https://monzo.com/", "https://community.monzo.com/", "https://www.monzo.com/", "https://monzo.org/",
                        "https://a.b.monzo.com/", "https://facebook.com/monzo", "https://monzo.com/ok")
                .page("https://monzo.com/ok");

        crawl(service(100), "https://monzo.com/");

        assertEquals(List.of("https://monzo.com/", "https://monzo.com/ok"), reporter.visited());
        assertEquals(0, site.downloads("https://community.monzo.com/"));
        assertEquals(0, site.downloads("https://www.monzo.com/"));
    }

    @Test
    void followsTheSameHostAcrossHttpAndHttps() throws Exception {
        site.page("http://monzo.com/", "https://monzo.com/secure").page("https://monzo.com/secure");

        crawl(service(100), "http://monzo.com/");

        assertEquals(List.of("http://monzo.com/", "https://monzo.com/secure"), reporter.visited());
    }

    @Test
    void visitsThePagesOfALinkWrittenInDifferentWaysOnce() throws Exception {
        site.page("http://monzo.com/", "http://monzo.com/a", "HTTP://MONZO.COM/a", "http://monzo.com/x/../a#top", "http://monzo.com/a")
                .page("http://monzo.com/a", "http://monzo.com");

        crawl(service(100), "http://monzo.com");

        assertEquals(List.of("http://monzo.com/", "http://monzo.com/a"), reporter.visited());
    }

    @Test
    void aPageThatFailsDoesNotStopTheCrawl() throws Exception {
        site.page("http://monzo.com/", "http://monzo.com/missing", "http://monzo.com/broken", "http://monzo.com/fine")
                .crash("http://monzo.com/broken", new IllegalStateException("parser bug"))
                .page("http://monzo.com/fine");

        var session = crawl(service(100), "http://monzo.com/");

        assertEquals(List.of("http://monzo.com/", "http://monzo.com/fine"), reporter.visited());
        assertEquals(2, session.pagesFailed());
        assertEquals(CrawlSession.State.FINISHED, session.state());
        assertEquals(1L, metrics.getMetrics().get("pageParseErrors"));
    }

    @Test
    void doesNotFollowTheLinksOfAPageThatRedirectedOutOfTheHost() throws Exception {
        site.redirect("http://monzo.com/", "https://www.monzo.com/", "https://www.monzo.com/a", "http://monzo.com/b")
                .page("https://www.monzo.com/a").page("http://monzo.com/b");

        crawl(service(100), "http://monzo.com/");

        assertEquals(List.of("http://monzo.com/"), reporter.visited());
        assertEquals(List.of("https://www.monzo.com/a", "http://monzo.com/b"), reporter.linksOf("http://monzo.com/"));
    }

    @Test
    void stopsAtTheMaximumNumberOfPages() throws Exception {
        site.page("http://big.com/", "http://big.com/1", "http://big.com/2", "http://big.com/3", "http://big.com/4")
                .page("http://big.com/1").page("http://big.com/2").page("http://big.com/3").page("http://big.com/4");

        var session = crawl(service(3), "http://big.com/");

        assertEquals(3, reporter.visited().size());
        assertEquals(3, session.pagesVisited());
        assertTrue(session.truncated());
        assertEquals(CrawlSession.State.FINISHED, session.state());
    }

    @Test
    void startingARunningCrawlDoesNothing() throws Exception {
        site.page("http://monzo.com/", "http://monzo.com/a").page("http://monzo.com/a");
        var service = service(100);

        var first = service.start("http://monzo.com/");
        var second = service.start("http://MONZO.com");

        assertEquals(CrawlerService.Start.STARTED, first.outcome());
        assertEquals(CrawlerService.Start.ALREADY_RUNNING, second.outcome());
        assertEquals(1, queue.size());
        assertEquals(CrawlSession.State.RUNNING, second.session().state());
        drain(service);
        assertEquals(2, reporter.visited().size());
    }

    @Test
    void aFinishedCrawlCanBeDoneAgain() throws Exception {
        site.page("http://monzo.com/", "http://monzo.com/a").page("http://monzo.com/a");
        var service = service(100);
        crawl(service, "http://monzo.com/");

        var again = service.start("http://monzo.com/");
        drain(service);

        assertEquals(CrawlerService.Start.STARTED, again.outcome());
        assertEquals(4, reporter.visited().size());
        assertEquals(1, service.sessions().size());
    }

    @Test
    void crawlingOneHostDoesNotDisturbAnother() throws Exception {
        site.page("http://a.com/", "http://a.com/1").page("http://a.com/1")
                .page("http://b.com/", "http://b.com/1").page("http://b.com/1");
        var service = service(100);
        service.start("http://a.com/");
        service.start("http://b.com/");
        service.start("http://a.com/"); // already running: nothing is forgotten
        drain(service);

        assertEquals(4, reporter.visited().size());
        assertEquals(2, service.sessions().size());
        assertEquals(2, visited.count("a.com"));
        assertEquals(2, visited.count("b.com"));
    }

    @Test
    void countsWhatItDoes() throws Exception {
        site.page("http://monzo.com/", "http://monzo.com/a", "http://monzo.com/a", "https://other.com/")
                .page("http://monzo.com/a", "http://monzo.com/");

        crawl(service(100), "http://monzo.com/");

        var counters = metrics.getMetrics();
        assertEquals(2L, counters.get("linkReceived")); // "/" and "/a"
        assertEquals(2L, counters.get("linkVisited"));
        assertEquals(4L, counters.get("linkParsed")); // 3 links in "/", 1 in "/a"
        assertEquals(1L, counters.get("linkToVisit")); // only "/a": the rest is external or already visited
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "monzo.com", "ftp://monzo.com", "mailto:a@b.c", "/relative", "http://"})
    void refusesWhatIsNotAWebUrl(String url) {
        var service = service(100);

        assertThrows(IllegalArgumentException.class, () -> service.start(url));
        assertEquals(0, queue.size());
        assertTrue(service.sessions().isEmpty());
    }
}
