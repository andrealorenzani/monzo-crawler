package name.lorenzani.andrea.monzo_crawler.fetch;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LinkExtractor {

    /** The absolute URL of all the links ({@code <a href>}) of the document, in order, with the repetitions. */
    public List<String> extract(Document html) {
        // https://jsoup.org/cookbook/extracting-data/selector-syntax
        return html.select("a[href]").stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> !url.isEmpty())
                .toList();
    }
}
