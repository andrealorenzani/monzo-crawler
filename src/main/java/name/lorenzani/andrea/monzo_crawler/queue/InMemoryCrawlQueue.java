package name.lorenzani.andrea.monzo_crawler.queue;

import name.lorenzani.andrea.monzo_crawler.model.CrawlLink;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class InMemoryCrawlQueue implements CrawlQueue {

    private final BlockingQueue<CrawlLink> queue = new LinkedBlockingQueue<>();

    @Override
    public void send(CrawlLink link) {
        queue.add(link);
    }

    @Override
    public Optional<CrawlLink> receive(Duration timeout) throws InterruptedException {
        return Optional.ofNullable(queue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS));
    }

    @Override
    public int size() {
        return queue.size();
    }
}
