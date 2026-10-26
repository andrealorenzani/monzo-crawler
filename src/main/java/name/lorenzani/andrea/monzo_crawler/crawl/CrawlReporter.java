package name.lorenzani.andrea.monzo_crawler.crawl;

import java.util.List;

/** Where the result of the crawl goes. */
public interface CrawlReporter {

    /** @param links every link found in the page, wherever it points to */
    void pageVisited(String url, List<String> links);
}
