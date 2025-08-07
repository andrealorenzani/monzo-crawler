package name.lorenzani.andrea.monzo_crawler.controllers;

import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.services.CacheService;
import name.lorenzani.andrea.monzo_crawler.services.MetricService;
import name.lorenzani.andrea.monzo_crawler.services.QueueService;
import name.lorenzani.andrea.monzo_crawler.utils.LinkUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CrawlerController {
    @Autowired CacheService cacheService;
    @Autowired QueueService queueService;
    @Autowired MetricService metricService;
    @Autowired LinkUtils linkUtils;

    @GetMapping("/")
    public Map<String, Object> status() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("pagesVisited", String.valueOf(cacheService.numPagesVisited()));
        result.put("queuedLinks", String.valueOf(queueService.numQueuedMessages()));
        result.putAll(metricService.getMetrics());
        result.put("Pages visited", cacheService.pagesVisited());
        return result;
    }

    @PostMapping("/crawl")
    public String crawl(@RequestParam(value = "url") String url) throws MalformedURLException, URISyntaxException {
        url = linkUtils.normalizeLink(url);
        cacheService.invalidate(url);
        queueService.sendMessage(new CrawlLink(url, url));
        return "URL enqueued";
    }
}
