package name.lorenzani.andrea.monzo_crawler.fetch;

import name.lorenzani.andrea.monzo_crawler.config.CrawlerProperties;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Downloads the pages with jsoup. */
@Component
public class JsoupPageFetcher implements PageFetcher {

    private final LinkExtractor linkExtractor;
    private final CrawlerProperties properties;

    public JsoupPageFetcher(LinkExtractor linkExtractor, CrawlerProperties properties) {
        this.linkExtractor = linkExtractor;
        this.properties = properties;
    }

    @Override
    public Page fetch(String url) throws IOException {
        var document = Jsoup.connect(url)
                .userAgent(properties.userAgent())
                .timeout((int) properties.requestTimeout().toMillis())
                .followRedirects(true)
                .get();
        return new Page(document.location(), linkExtractor.extract(document));
    }
}
