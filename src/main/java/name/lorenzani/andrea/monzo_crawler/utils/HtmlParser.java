package name.lorenzani.andrea.monzo_crawler.utils;


import lombok.extern.log4j.Log4j2;
import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import name.lorenzani.andrea.monzo_crawler.services.MetricService;
import name.lorenzani.andrea.monzo_crawler.utils.htmlprocessors.LinkExtractor;
import org.apache.logging.log4j.LogManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Log4j2
public class HtmlParser {

    @Autowired LinkExtractor linkExtractor;
    @Autowired
    MetricService metricService;

    public List<String> extractLinks(CrawlLink link) {
        try {
            Document doc = Jsoup.connect(link.getCurrentLink()).timeout(5000).get();
            return linkExtractor.process(doc);
        } catch(IOException ex) {
            LogManager.getLogger(getClass()).error(ex);
            metricService.incrementParseErrors();
        }
        return List.of();
    }
}
