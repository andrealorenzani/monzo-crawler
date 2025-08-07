package name.lorenzani.andrea.monzo_crawler.utils.htmlprocessors;

import org.jsoup.nodes.Document;

public interface DocumentProcessor<T> {
    T process(Document input);
}
