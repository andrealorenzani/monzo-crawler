package name.lorenzani.andrea.monzo_crawler.services;

import lombok.extern.log4j.Log4j2;
import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.utils.HtmlParser;
import name.lorenzani.andrea.monzo_crawler.utils.linkfilters.LinkFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CrawlerService {
    @Autowired private LinkFilter linkFilter;
    @Autowired private HtmlParser htmlParser;
    @Autowired private QueueService queueService;
    @Autowired private CacheService cacheService;
    @Autowired private MetricService metricService;

    protected AtomicBoolean isActive = new AtomicBoolean(true);

    // This method simulates the active polling of this service from AWS SQS
    // (or any activeMQ). To simulate it, I just create an infinite loop where
    // if there is a new message, it is processed, if not we wait one second
    @Async
    public void listenToQueue() {
        String threadName = UUID.randomUUID().toString();
        log.info("Starting {}", threadName);
        while(isActive.get()) {
            try {
                CrawlLink message = queueService.receiveMessage();
                if (Objects.isNull(message)) {
                    Thread.sleep(1000);
                } else {
                    log.debug("{} - Received message: {}", threadName, message);
                    metricService.incrementLinkReceived();
                    crawl(threadName, message);
                }
            }
            catch(InterruptedException ie) {
                log.error(threadName+" - Error processing: ", ie);
                log.error("Maybe a shutdown? Killing the thread");
                break;
            }
        }
        log.info("Stopping {}", threadName);
    }

    public void crawl(String threadName, CrawlLink inputUrl) {
        if(linkFilter.filter(inputUrl) &&
                Objects.isNull(cacheService.addVisited(inputUrl.getCurrentLink()))) {
            metricService.incrementLinkVisited();
            List<String> links = htmlParser.extractLinks(inputUrl);
            List<CrawlLink> toBeEnqueued = links.stream()
                    .map(nextUrl -> new CrawlLink(inputUrl.getDomain(), nextUrl))
                    .filter(linkFilter::filter)
                    .toList();
            metricService.incrementLinkToVisit(toBeEnqueued.size());
            toBeEnqueued.forEach(queueService::sendMessage);
            String foundLinks = links.stream().map(link -> "\t\t"+link).collect(Collectors.joining("\n"));
            log.info("{} - Visited {} - found \n{}", threadName, inputUrl.getCurrentLink(), foundLinks);
            metricService.incrementLinkParsed(links.size());
        }
    }

}
