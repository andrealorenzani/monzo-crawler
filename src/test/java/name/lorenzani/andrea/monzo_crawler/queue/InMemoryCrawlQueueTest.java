package name.lorenzani.andrea.monzo_crawler.queue;

import name.lorenzani.andrea.monzo_crawler.model.CrawlLink;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCrawlQueueTest {

    private final InMemoryCrawlQueue queue = new InMemoryCrawlQueue();

    @Test
    void deliversInOrder() throws Exception {
        var first = new CrawlLink("a.com", "http://a.com/1");
        var second = new CrawlLink("a.com", "http://a.com/2");
        queue.send(first);
        queue.send(second);

        assertEquals(2, queue.size());
        assertEquals(Optional.of(first), queue.receive(Duration.ZERO));
        assertEquals(Optional.of(second), queue.receive(Duration.ZERO));
        assertEquals(0, queue.size());
    }

    @Test
    void anEmptyQueueAnswersAfterTheTimeout() throws Exception {
        long start = System.nanoTime();

        assertEquals(Optional.empty(), queue.receive(Duration.ofMillis(50)));

        assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() >= 45);
    }

    @Test
    void aWaitingReceiverGetsTheLinkAsSoonAsItArrives() throws Exception {
        var link = new CrawlLink("a.com", "http://a.com/");
        var waiting = CompletableFuture.supplyAsync(() -> {
            try {
                return queue.receive(Duration.ofSeconds(10));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        Thread.sleep(50);

        queue.send(link);

        assertEquals(Optional.of(link), waiting.get());
    }
}
