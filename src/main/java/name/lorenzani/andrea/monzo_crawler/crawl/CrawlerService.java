package name.lorenzani.andrea.monzo_crawler.crawl;

import name.lorenzani.andrea.monzo_crawler.config.CrawlerProperties;
import name.lorenzani.andrea.monzo_crawler.fetch.Page;
import name.lorenzani.andrea.monzo_crawler.fetch.PageFetcher;
import name.lorenzani.andrea.monzo_crawler.metrics.MetricService;
import name.lorenzani.andrea.monzo_crawler.model.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.queue.CrawlQueue;
import name.lorenzani.andrea.monzo_crawler.visited.VisitedStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A breadth first crawl of one host. {@link #start} puts the first link in the queue; the workers take links from the
 * queue and call {@link #process}, which downloads the page, reports it and queues the links to pages of the same host
 * that nobody visited yet. Nothing here is shared between instances except the queue and the visited store, so more
 * instances can work on the same crawl when those are external.
 */
@Service
public class CrawlerService {

    public enum Start { STARTED, ALREADY_RUNNING }

    public record StartResult(Start outcome, CrawlSession session) {
    }

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private final CrawlQueue queue;
    private final VisitedStore visited;
    private final PageFetcher fetcher;
    private final CrawlReporter reporter;
    private final MetricService metrics;
    private final CrawlerProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, CrawlSession> sessions = new ConcurrentHashMap<>();

    public CrawlerService(CrawlQueue queue, VisitedStore visited, PageFetcher fetcher, CrawlReporter reporter,
                          MetricService metrics, CrawlerProperties properties, Clock clock) {
        this.queue = queue;
        this.visited = visited;
        this.fetcher = fetcher;
        this.reporter = reporter;
        this.metrics = metrics;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Starts the crawl of the host of the URL, which is the first page. If that host is being crawled already, nothing
     * happens: a crawl that is over is forgotten and done again.
     *
     * @throws IllegalArgumentException if the URL is not an absolute http(s) URL
     */
    public StartResult start(String rawUrl) {
        String url = Links.normalize(rawUrl)
                .orElseThrow(() -> new IllegalArgumentException("Not an absolute http or https URL: " + rawUrl));
        String host = Links.host(url);
        boolean[] created = {false};
        CrawlSession session = sessions.compute(host, (h, current) -> {
            if (current != null && current.state() == CrawlSession.State.RUNNING) {
                return current;
            }
            created[0] = true;
            var fresh = new CrawlSession(host, url, clock.instant(), properties.maxPagesPerCrawl());
            visited.forget(host);
            fresh.linkQueued();
            return fresh;
        });
        if (!created[0]) {
            return new StartResult(Start.ALREADY_RUNNING, session);
        }
        queue.send(new CrawlLink(host, url));
        log.info("Crawl of {} started from {}", host, url);
        return new StartResult(Start.STARTED, session);
    }

    public Collection<CrawlSession> sessions() {
        return sessions.values();
    }

    public Optional<CrawlSession> session(String host) {
        return Optional.ofNullable(sessions.get(host));
    }

    /** Visits one link taken from the queue. It never throws: a failing page must not stop the worker. */
    public void process(CrawlLink link) {
        metrics.incrementLinkReceived();
        CrawlSession session = sessions.get(link.host());
        if (session == null) {
            return;
        }
        try {
            visit(session, link);
        } catch (RuntimeException e) {
            session.pageFailed();
            log.error("Unexpected error visiting {}", link.url(), e);
        } finally {
            if (session.linkDone(clock.instant())) {
                log.info("Crawl of {} finished: {} pages visited, {} failed{}", session.host(), session.pagesVisited(),
                        session.pagesFailed(), session.truncated() ? " (stopped at the maximum number of pages)" : "");
            }
        }
    }

    private void visit(CrawlSession session, CrawlLink link) {
        // Only the first worker that gets a link visits it
        if (!visited.markVisited(link.host(), link.url()) || !session.reservePage()) {
            return;
        }
        metrics.incrementLinkVisited();
        Page page;
        try {
            page = fetcher.fetch(link.url());
        } catch (IOException e) {
            session.pageFailed();
            metrics.incrementParseErrors();
            log.warn("Unable to visit {}: {}", link.url(), e.toString());
            return;
        }
        reporter.pageVisited(link.url(), page.links());
        metrics.incrementLinkParsed(page.links().size());

        // A redirect to another host (monzo.com -> www.monzo.com) takes us out of the crawl: its links are not ours
        boolean stillOnHost = Links.normalize(page.finalUrl()).map(Links::host).filter(link.host()::equals).isPresent();
        if (!stillOnHost) {
            log.info("{} redirects to {}, which is out of {}: its links are not followed", link.url(), page.finalUrl(), link.host());
            return;
        }
        var toVisit = new LinkedHashSet<String>();
        for (String found : page.links()) {
            Links.normalize(found)
                    .filter(url -> Links.host(url).equals(link.host()))
                    .filter(url -> !visited.isVisited(link.host(), url))
                    .ifPresent(toVisit::add);
        }
        metrics.incrementLinkToVisit(toVisit.size());
        toVisit.forEach(url -> {
            session.linkQueued();
            queue.send(new CrawlLink(link.host(), url));
        });
    }
}
