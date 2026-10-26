package name.lorenzani.andrea.monzo_crawler.crawl;

import name.lorenzani.andrea.monzo_crawler.config.CrawlerProperties;
import name.lorenzani.andrea.monzo_crawler.queue.CrawlQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The workers: threads that take a link from the queue, visit it and start again. In production they would be
 * more instances of the application (or lambdas) reading from the same queue. Each one is a virtual thread, which
 * costs nothing while it waits for the network.
 */
@Component
public class CrawlerWorkers implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CrawlerWorkers.class);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(10);

    private final CrawlQueue queue;
    private final CrawlerService crawler;
    private final CrawlerProperties properties;
    private final List<Thread> threads = new ArrayList<>();
    private volatile boolean running;

    public CrawlerWorkers(CrawlQueue queue, CrawlerService crawler, CrawlerProperties properties) {
        this.queue = queue;
        this.crawler = crawler;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        running = true;
        for (int i = 0; i < properties.workers(); i++) {
            threads.add(Thread.ofVirtual().name("crawler-" + i).start(this::work));
        }
        log.info("Started {} crawler workers", threads.size());
    }

    private void work() {
        while (running) {
            try {
                queue.receive(properties.pollTimeout()).ifPresent(crawler::process);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.error("The worker failed to take a link from the queue", e);
            }
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            try {
                thread.join(STOP_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        threads.clear();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
