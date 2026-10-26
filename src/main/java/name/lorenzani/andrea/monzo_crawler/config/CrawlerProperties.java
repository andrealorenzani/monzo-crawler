package name.lorenzani.andrea.monzo_crawler.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param workers          how many pages are downloaded at the same time (0 starts no worker)
 * @param maxPagesPerCrawl a crawl stops visiting pages after this many, whatever the size of the site
 * @param requestTimeout   how long to wait for a page
 * @param pollTimeout      how long a worker waits for a link to visit before checking whether it has to stop
 * @param userAgent        how the crawler introduces itself to the sites
 */
@Validated
@ConfigurationProperties(prefix = "crawler")
public record CrawlerProperties(
        @DefaultValue("4") @Min(0) int workers,
        @DefaultValue("10000") @Positive int maxPagesPerCrawl,
        @DefaultValue("5s") Duration requestTimeout,
        @DefaultValue("500ms") Duration pollTimeout,
        @DefaultValue("monzo-crawler") @NotBlank String userAgent) {
}
