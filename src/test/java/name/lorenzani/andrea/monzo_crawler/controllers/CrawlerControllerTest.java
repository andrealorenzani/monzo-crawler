package name.lorenzani.andrea.monzo_crawler.controllers;

import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.services.CacheService;
import name.lorenzani.andrea.monzo_crawler.services.MetricService;
import name.lorenzani.andrea.monzo_crawler.services.QueueService;
import name.lorenzani.andrea.monzo_crawler.utils.LinkUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlerControllerTest {

    @InjectMocks CrawlerController underTest;
    @Mock CacheService cacheService;
    @Mock QueueService queueService;
    @Mock MetricService metricService;
    @Mock LinkUtils linkUtils;

    @Captor ArgumentCaptor<CrawlLink> linkCaptured;

    @Test
    public void testEndpointEnqueueNormalisedLink() throws MalformedURLException, URISyntaxException {
        String inputlink = "test";
        String normalisedlink = "test2";
        when(linkUtils.normalizeLink(inputlink)).thenReturn(normalisedlink);
        underTest.crawl(inputlink);
        verify(linkUtils).normalizeLink(inputlink);
        verify(cacheService).invalidate(normalisedlink);
        verify(queueService).sendMessage(linkCaptured.capture());
        assertEquals(normalisedlink, linkCaptured.getValue().getDomain());
        assertEquals(normalisedlink, linkCaptured.getValue().getCurrentLink());
    }

}