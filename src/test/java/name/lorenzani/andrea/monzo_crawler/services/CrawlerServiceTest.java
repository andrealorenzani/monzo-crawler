package name.lorenzani.andrea.monzo_crawler.services;

import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.utils.HtmlParser;
import name.lorenzani.andrea.monzo_crawler.utils.linkfilters.LinkFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {
    @InjectMocks private CrawlerService underTest;

    @Mock private LinkFilter linkFilter;
    @Mock private HtmlParser htmlParser;
    @Mock private QueueService queueService;
    @Mock private CacheService cacheService;
    @Mock private MetricService metricService;
    @Mock private AtomicBoolean isActive;

    @Test
    public void testDataExtraction() {
        String url = "http://monzo.com";
        CrawlLink link = new CrawlLink(url, url);
        underTest.isActive = isActive;
        when(queueService.receiveMessage()).thenReturn(link, null);
        when(isActive.get()).thenReturn(true, true, false);
        when(linkFilter.filter(link)).thenReturn(false);
        underTest.listenToQueue();
        verify(queueService, times(2)).receiveMessage();
        verify(linkFilter).filter(link);
        verify(metricService).incrementLinkReceived();
        verify(metricService, never()).incrementDecodeErrors();
    }

    @Test
    public void testCrawlFilteredOut() {
        String url = "http://monzo.com", threadName = "thread";
        CrawlLink link = new CrawlLink(url, url);
        underTest.isActive = isActive;
        when(linkFilter.filter(link)).thenReturn(false);
        underTest.crawl(threadName, link);
        verify(cacheService, never()).addVisited(anyString());
        verify(metricService, never()).incrementLinkVisited();
    }

    @Test
    public void testConcurrentVisitIsStopped() {
        String url = "http://monzo.com", threadName = "thread";
        CrawlLink link = new CrawlLink(url, url);
        underTest.isActive = isActive;
        when(linkFilter.filter(link)).thenReturn(true);
        when(cacheService.addVisited(link.getCurrentLink())).thenReturn("Already added to visited");
        underTest.crawl(threadName, link);
        verify(cacheService).addVisited(anyString());
        verify(metricService, never()).incrementLinkVisited();
    }

    @Test
    public void testHappyPath() {
        String url = "http://monzo.com", threadName = "thread";
        CrawlLink link = new CrawlLink(url, url);
        underTest.isActive = isActive;
        when(linkFilter.filter(any())).thenReturn(true, true, true, false);
        when(htmlParser.extractLinks(any())).thenReturn(List.of("Link1", "Link2", "Link3"));
        underTest.crawl(threadName, link);
        verify(cacheService).addVisited(anyString());
        verify(metricService).incrementLinkVisited();
        verify(htmlParser).extractLinks(link);
        verify(metricService).incrementLinkToVisit(2);
        verify(queueService, times(2)).sendMessage(any());
        verify(metricService).incrementLinkParsed(3);
    }

    @Test
    public void testNoNewUrlToProcess() {
        String url = "http://monzo.com", threadName = "thread";
        CrawlLink link = new CrawlLink(url, url);
        underTest.isActive = isActive;
        when(linkFilter.filter(any())).thenReturn(true, false);
        when(htmlParser.extractLinks(any())).thenReturn(List.of("Link1"));
        underTest.crawl(threadName, link);
        verify(cacheService).addVisited(anyString());
        verify(metricService).incrementLinkVisited();
        verify(htmlParser).extractLinks(link);
        verify(metricService).incrementLinkToVisit(0);
        verify(queueService, never()).sendMessage(any());
        verify(metricService).incrementLinkParsed(1);
    }

    @Test
    public void testNoLinkProcessed() {
        String url = "http://monzo.com", threadName = "thread";
        CrawlLink link = new CrawlLink(url, url);
        underTest.isActive = isActive;
        when(linkFilter.filter(any())).thenReturn(true, false);
        when(htmlParser.extractLinks(any())).thenReturn(List.of());
        underTest.crawl(threadName, link);
        verify(cacheService).addVisited(anyString());
        verify(metricService).incrementLinkVisited();
        verify(htmlParser).extractLinks(link);
        verify(metricService).incrementLinkToVisit(0);
        verify(queueService, never()).sendMessage(any());
        verify(metricService).incrementLinkParsed(0);
    }

}