package name.lorenzani.andrea.monzo_crawler.crawl;

import org.springframework.stereotype.Component;

import java.util.List;

/** Prints each visited page and the links it contains. */
@Component
public class ConsoleCrawlReporter implements CrawlReporter {

    @Override
    public void pageVisited(String url, List<String> links) {
        var text = new StringBuilder("Visited ").append(url).append(" - found ").append(links.size()).append(" links");
        links.forEach(link -> text.append("\n\t").append(link));
        // one call, so that the output of different workers is never mixed
        System.out.println(text);
    }
}
