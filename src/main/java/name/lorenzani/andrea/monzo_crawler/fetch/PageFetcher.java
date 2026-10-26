package name.lorenzani.andrea.monzo_crawler.fetch;

import java.io.IOException;

public interface PageFetcher {

    /** @throws IOException when the page can't be downloaded or is not an HTML page */
    Page fetch(String url) throws IOException;
}
