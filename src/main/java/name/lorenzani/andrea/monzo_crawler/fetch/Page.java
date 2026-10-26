package name.lorenzani.andrea.monzo_crawler.fetch;

import java.util.List;

/**
 * @param finalUrl where the page really was, after the redirects
 * @param links    the absolute URL of every link of the page, in the order they appear
 */
public record Page(String finalUrl, List<String> links) {
}
