package name.lorenzani.andrea.monzo_crawler.utils.linkfilters;

import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.services.CacheService;
import name.lorenzani.andrea.monzo_crawler.utils.LinkUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkFilterTest {

    @Mock CacheService cacheService;
    @Mock LinkUtils linkUtils;
    @InjectMocks
    LinkFilter underTest;

    @Test
    public void testGenericFilter() {
        String linkDomain = "http://monzo.com";
        String linkCurrent = "http://monzo.com/anypage";
        CrawlLink link = new CrawlLink(linkDomain, linkCurrent);

        when(linkUtils.normalizeLink(linkCurrent)).thenReturn(linkCurrent);
        when(cacheService.isVisited(linkCurrent)).thenReturn(false);
        assertTrue(underTest.filter(link));
    }

    @Test
    public void testNoSubdomain() {
        String linkDomain = "http://monzo.com";
        String linkCurrent = "http://subdomain.monzo.com/anypage";
        CrawlLink link = new CrawlLink(linkDomain, linkCurrent);

        when(linkUtils.normalizeLink(linkCurrent)).thenReturn(linkCurrent);
        when(cacheService.isVisited(linkCurrent)).thenReturn(false);
        assertFalse(underTest.filter(link));
    }

    @Test
    public void testNoWWW() {
        String linkDomain = "http://monzo.com";
        String linkCurrent = "http://www.monzo.com/anypage";
        CrawlLink link = new CrawlLink(linkDomain, linkCurrent);

        when(linkUtils.normalizeLink(linkCurrent)).thenReturn(linkCurrent);
        when(cacheService.isVisited(linkCurrent)).thenReturn(false);
        assertFalse(underTest.filter(link));
    }

    @Test
    public void testAcceptHttpToHttps() {
        String linkDomain = "http://monzo.com";
        String linkCurrent = "https://monzo.com/anypage";
        CrawlLink link = new CrawlLink(linkDomain, linkCurrent);

        when(linkUtils.normalizeLink(linkCurrent)).thenReturn(linkCurrent);
        when(cacheService.isVisited(linkCurrent)).thenReturn(false);
        assertTrue(underTest.filter(link));
    }

    @Test
    public void testNotAlreadyVisited() {
        String linkDomain = "http://monzo.com";
        String linkCurrent = "http://monzo.com/anypage";
        CrawlLink link = new CrawlLink(linkDomain, linkCurrent);

        when(linkUtils.normalizeLink(linkCurrent)).thenReturn(linkCurrent);
        when(cacheService.isVisited(linkCurrent)).thenReturn(true);
        assertFalse(underTest.filter(link));
    }

}