package name.lorenzani.andrea.monzo_crawler;

import name.lorenzani.andrea.monzo_crawler.crawl.CrawlReporter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Remembers what was reported. */
public class RecordingReporter implements CrawlReporter {

    private final List<String> visitedInOrder = new CopyOnWriteArrayList<>();
    private final Map<String, List<String>> linksByPage = new ConcurrentHashMap<>();

    @Override
    public void pageVisited(String url, List<String> links) {
        visitedInOrder.add(url);
        linksByPage.put(url, links);
    }

    public List<String> visited() {
        return List.copyOf(visitedInOrder);
    }

    public List<String> linksOf(String url) {
        return linksByPage.get(url);
    }
}
