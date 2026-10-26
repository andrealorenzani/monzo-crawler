package name.lorenzani.andrea.monzo_crawler.crawl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import name.lorenzani.andrea.monzo_crawler.FakeSite;
import name.lorenzani.andrea.monzo_crawler.RecordingReporter;
import name.lorenzani.andrea.monzo_crawler.config.CrawlerProperties;
import name.lorenzani.andrea.monzo_crawler.fetch.Page;
import name.lorenzani.andrea.monzo_crawler.metrics.MetricService;
import name.lorenzani.andrea.monzo_crawler.queue.InMemoryCrawlQueue;
import name.lorenzani.andrea.monzo_crawler.visited.InMemoryVisitedStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Several workers, taking links from the same queue, at the same time. */
class CrawlerWorkersTest {

    private static final int PAGES = 400;

    private static String pageUrl(int i) {
        return "http://big.com/" + i;
    }

    @Test
    void manyWorkersVisitEachPageOnceAndTheCrawlFinishes() throws Exception {
        // A site with cycles and many links to the same pages: the page i links to i+1, 2i+1 and 3i+2
        var site = new FakeSite() {
            @Override
            public Page fetch(String url) throws IOException {
                countDownload(url);
                try {
                    Thread.sleep(1); // downloading takes time: the workers overlap
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                int i = Integer.parseInt(url.substring(url.lastIndexOf('/') + 1));
                Set<String> links = new HashSet<>();
                for (int next : new int[]{i + 1, 2 * i + 1, 3 * i + 2, i}) {
                    if (next < PAGES) {
                        links.add(pageUrl(next));
                    }
                }
                links.add("http://elsewhere.com/" + i);
                return new Page(url, links.stream().toList());
            }
        };
        var queue = new InMemoryCrawlQueue();
        var reporter = new RecordingReporter();
        var properties = new CrawlerProperties(8, PAGES * 2, Duration.ofSeconds(1), Duration.ofMillis(20), "test");
        var crawler = new CrawlerService(queue, new InMemoryVisitedStore(), site, reporter,
                new MetricService(new SimpleMeterRegistry()), properties, Clock.systemUTC());
        var workers = new CrawlerWorkers(queue, crawler, properties);

        workers.start();
        try {
            var session = crawler.start(pageUrl(0)).session();
            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (session.state() == CrawlSession.State.RUNNING && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }

            assertEquals(CrawlSession.State.FINISHED, session.state(), "the crawl did not finish");
            assertEquals(PAGES, session.pagesVisited());
            assertEquals(PAGES, reporter.visited().size());
            assertEquals(PAGES, new HashSet<>(reporter.visited()).size(), "a page was reported twice");
            IntStream.range(0, PAGES).forEach(i -> assertEquals(1, site.downloads(pageUrl(i)), pageUrl(i)));
            assertEquals(0, queue.size());
        } finally {
            workers.stop();
        }
        assertFalse(workers.isRunning());
    }

    @Test
    void aWorkerSurvivesTheFailureOfAPage() throws Exception {
        var site = new FakeSite()
                .page("http://monzo.com/", "http://monzo.com/bad", "http://monzo.com/good")
                .crash("http://monzo.com/bad", new IllegalStateException("boom"))
                .page("http://monzo.com/good");
        var queue = new InMemoryCrawlQueue();
        var reporter = new RecordingReporter();
        var properties = new CrawlerProperties(1, 100, Duration.ofSeconds(1), Duration.ofMillis(20), "test");
        var crawler = new CrawlerService(queue, new InMemoryVisitedStore(), site, reporter,
                new MetricService(new SimpleMeterRegistry()), properties, Clock.systemUTC());
        var workers = new CrawlerWorkers(queue, crawler, properties);

        workers.start();
        try {
            var session = crawler.start("http://monzo.com/").session();
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (session.state() == CrawlSession.State.RUNNING && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }

            assertEquals(CrawlSession.State.FINISHED, session.state());
            assertTrue(reporter.visited().contains("http://monzo.com/good"));
        } finally {
            workers.stop();
        }
    }

    @Test
    void withoutWorkersNothingHappens() {
        var properties = new CrawlerProperties(0, 100, Duration.ofSeconds(1), Duration.ofMillis(20), "test");
        var workers = new CrawlerWorkers(new InMemoryCrawlQueue(), null, properties);

        workers.start();
        workers.stop();

        assertFalse(workers.isRunning());
    }
}
