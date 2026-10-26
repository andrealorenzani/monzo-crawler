package name.lorenzani.andrea.monzo_crawler;

import name.lorenzani.andrea.monzo_crawler.fetch.Page;
import name.lorenzani.andrea.monzo_crawler.fetch.PageFetcher;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** A website in memory: the page at each URL and the links it contains. */
public class FakeSite implements PageFetcher {

    private final Map<String, Page> pages = new ConcurrentHashMap<>();
    private final Map<String, RuntimeException> crashes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> downloads = new ConcurrentHashMap<>();

    public FakeSite page(String url, String... links) {
        pages.put(url, new Page(url, List.of(links)));
        return this;
    }

    /** A page that, once the redirects are followed, is at another URL. */
    public FakeSite redirect(String url, String finalUrl, String... links) {
        pages.put(url, new Page(finalUrl, List.of(links)));
        return this;
    }

    public FakeSite crash(String url, RuntimeException failure) {
        crashes.put(url, failure);
        return this;
    }

    /** For the subclasses that replace {@link #fetch}. */
    protected void countDownload(String url) {
        downloads.computeIfAbsent(url, u -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    public Page fetch(String url) throws IOException {
        countDownload(url);
        if (crashes.containsKey(url)) {
            throw crashes.get(url);
        }
        Page page = pages.get(url);
        if (page == null) {
            throw new IOException("HTTP error fetching URL. Status=404, URL=[" + url + "]");
        }
        return page;
    }

    public int downloads(String url) {
        return downloads.getOrDefault(url, new AtomicInteger()).get();
    }

    public Map<String, AtomicInteger> allDownloads() {
        return downloads;
    }
}
