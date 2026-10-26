package name.lorenzani.andrea.monzo_crawler.visited;

/**
 * The pages already visited, per host. In production this is a cache with atomic operations (Redis, ...): what matters
 * is that {@link #markVisited} tells exactly one caller that it was the first.
 */
public interface VisitedStore {

    /** @return true if the URL was not visited before: only one of the concurrent callers gets true */
    boolean markVisited(String host, String url);

    boolean isVisited(String host, String url);

    int count(String host);

    /** Forgets what was visited on the host, to crawl it again. */
    void forget(String host);
}
