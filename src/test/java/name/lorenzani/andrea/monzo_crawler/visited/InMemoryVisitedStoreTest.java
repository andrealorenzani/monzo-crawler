package name.lorenzani.andrea.monzo_crawler.visited;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVisitedStoreTest {

    private final InMemoryVisitedStore store = new InMemoryVisitedStore();

    @Test
    void onlyTheFirstVisitCounts() {
        assertFalse(store.isVisited("a.com", "http://a.com/"));

        assertTrue(store.markVisited("a.com", "http://a.com/"));
        assertFalse(store.markVisited("a.com", "http://a.com/"));

        assertTrue(store.isVisited("a.com", "http://a.com/"));
        assertEquals(1, store.count("a.com"));
    }

    @Test
    void hostsAreIndependent() {
        store.markVisited("a.com", "http://a.com/");
        store.markVisited("b.com", "http://b.com/");
        store.markVisited("b.com", "http://b.com/x");

        store.forget("b.com");

        assertEquals(1, store.count("a.com"));
        assertEquals(0, store.count("b.com"));
        assertTrue(store.markVisited("b.com", "http://b.com/"));
    }

    @Test
    void exactlyOneOfTheConcurrentCallersIsFirst() throws Exception {
        try (var executor = Executors.newFixedThreadPool(16)) {
            var firsts = new CopyOnWriteArrayList<Boolean>();
            var futures = IntStream.range(0, 200)
                    .mapToObj(i -> executor.submit(() -> firsts.add(store.markVisited("a.com", "http://a.com/same"))))
                    .toList();
            for (var future : futures) {
                future.get();
            }

            assertEquals(1, firsts.stream().filter(first -> first).count());
        }
    }

    @Test
    void unknownHostsAreEmpty() {
        assertEquals(0, store.count("nobody.com"));
        assertFalse(store.isVisited("nobody.com", "x"));
        store.forget("nobody.com");
        assertEquals(new ConcurrentHashMap<>().size(), store.count("nobody.com"));
    }
}
