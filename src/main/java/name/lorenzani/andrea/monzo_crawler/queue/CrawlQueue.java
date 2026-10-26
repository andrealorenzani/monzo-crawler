package name.lorenzani.andrea.monzo_crawler.queue;

import name.lorenzani.andrea.monzo_crawler.model.CrawlLink;

import java.time.Duration;
import java.util.Optional;

/**
 * The links that still have to be visited. In production this is a managed queue (SQS, ...); the in-memory
 * implementation behaves the same way for one instance.
 */
public interface CrawlQueue {

    void send(CrawlLink link);

    /** Waits up to {@code timeout} for a link. */
    Optional<CrawlLink> receive(Duration timeout) throws InterruptedException;

    int size();
}
