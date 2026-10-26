package name.lorenzani.andrea.monzo_crawler.model;

/**
 * A page to visit.
 *
 * @param host the host of the page where the crawl started: the crawl never leaves it
 * @param url  the normalised URL of the page to visit
 */
public record CrawlLink(String host, String url) {
}
