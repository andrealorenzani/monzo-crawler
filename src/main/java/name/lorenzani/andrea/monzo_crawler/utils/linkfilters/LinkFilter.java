package name.lorenzani.andrea.monzo_crawler.utils.linkfilters;

import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.services.CacheService;
import name.lorenzani.andrea.monzo_crawler.utils.LinkUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class LinkFilter {

    @Autowired private CacheService cacheService;
    @Autowired private LinkUtils linkUtils;

    public boolean filter(CrawlLink link) {
        link.setCurrentLink(linkUtils.normalizeLink(link.getCurrentLink()));
        return filterAlreadyVisited(link) &&
                filterSameDomain(link);
    }

    protected boolean filterAlreadyVisited(CrawlLink link) {
        return !cacheService.isVisited(link.getCurrentLink());
    }

    protected boolean filterSameDomain(CrawlLink link) {
        try {
            return new URI(link.getDomain()).normalize().getHost().equals(
                    new URI(link.getCurrentLink()).normalize().getHost());
        }
        catch (URISyntaxException e) {
            LogManager.getLogger(getClass()).error(e);
        }
        return false;
    }
}
