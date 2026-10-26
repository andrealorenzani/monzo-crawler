package name.lorenzani.andrea.monzo_crawler.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

/** The counters of the crawler, available from the actuator ({@code /actuator/metrics/monzoCustomMetric.*}). */
@Service
public class MetricService {

    static final String METRIC_LINK_RECEIVED = "linkReceived";
    static final String METRIC_LINK_PARSED = "linkParsed";
    static final String METRIC_LINK_VISITED = "linkVisited";
    static final String METRIC_LINK_TO_VISIT = "linkToVisit";
    static final String METRIC_PARSE_ERRORS = "pageParseErrors";
    static final String METRIC_GROUP = "monzoCustomMetric.";

    private final MeterRegistry meterRegistry;

    public MetricService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementLinkReceived() {
        increment(METRIC_LINK_RECEIVED, 1);
    }

    public void incrementLinkVisited() {
        increment(METRIC_LINK_VISITED, 1);
    }

    public void incrementParseErrors() {
        increment(METRIC_PARSE_ERRORS, 1);
    }

    public void incrementLinkToVisit(int links) {
        increment(METRIC_LINK_TO_VISIT, links);
    }

    public void incrementLinkParsed(int links) {
        increment(METRIC_LINK_PARSED, links);
    }

    private void increment(String name, int value) {
        meterRegistry.counter(METRIC_GROUP + name).increment(value);
    }

    /** The value of each counter, by name (without the group). */
    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new TreeMap<>();
        Search.in(meterRegistry).name(name -> name.startsWith(METRIC_GROUP)).counters()
                .forEach(counter -> metrics.put(counter.getId().getName().substring(METRIC_GROUP.length()),
                        (long) counter.count()));
        return metrics;
    }
}
