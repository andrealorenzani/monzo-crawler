package name.lorenzani.andrea.monzo_crawler.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrawlLink {
    String domain;
    String currentLink;
}
