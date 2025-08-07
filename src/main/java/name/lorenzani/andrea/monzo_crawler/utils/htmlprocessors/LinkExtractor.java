package name.lorenzani.andrea.monzo_crawler.utils.htmlprocessors;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LinkExtractor implements DocumentProcessor<List<String>> {

    @Override
    public List<String> process(Document html) {
        // https://jsoup.org/cookbook/extracting-data/selector-syntax
        Elements links = html.select("a[href]");
        return links.stream().map(link -> link.absUrl("href"))
                .collect(Collectors.toList());
    }
}
