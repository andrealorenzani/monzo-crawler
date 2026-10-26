package name.lorenzani.andrea.monzo_crawler.crawl;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** The progress of the crawl of one host. */
public class CrawlSession {

    public enum State { RUNNING, FINISHED }

    private final String host;
    private final String startUrl;
    private final Instant startedAt;
    private final int maxPages;
    // The links in the queue plus the ones being visited: when it reaches zero the crawl is over
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicInteger pagesVisited = new AtomicInteger();
    private final AtomicInteger pagesFailed = new AtomicInteger();
    private final AtomicReference<Instant> finishedAt = new AtomicReference<>();
    private volatile boolean truncated;

    public CrawlSession(String host, String startUrl, Instant startedAt, int maxPages) {
        this.host = host;
        this.startUrl = startUrl;
        this.startedAt = startedAt;
        this.maxPages = maxPages;
    }

    public String host() {
        return host;
    }

    public String startUrl() {
        return startUrl;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public State state() {
        return finishedAt.get() == null ? State.RUNNING : State.FINISHED;
    }

    public int pagesVisited() {
        return pagesVisited.get();
    }

    public int pagesFailed() {
        return pagesFailed.get();
    }

    public int pending() {
        return pending.get();
    }

    /** True when the crawl stopped because it reached the maximum number of pages. */
    public boolean truncated() {
        return truncated;
    }

    void linkQueued() {
        pending.incrementAndGet();
    }

    /** @return true when that was the last link of the crawl */
    boolean linkDone(Instant now) {
        if (pending.decrementAndGet() == 0) {
            finishedAt.compareAndSet(null, now);
            return true;
        }
        return false;
    }

    /** @return false when the maximum number of pages was already reached */
    boolean reservePage() {
        if (pagesVisited.incrementAndGet() > maxPages) {
            pagesVisited.decrementAndGet();
            truncated = true;
            return false;
        }
        return true;
    }

    void pageFailed() {
        pagesFailed.incrementAndGet();
    }
}
