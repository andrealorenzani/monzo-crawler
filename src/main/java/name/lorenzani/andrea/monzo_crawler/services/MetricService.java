package name.lorenzani.andrea.monzo_crawler.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Log4j2
public class MetricService {
    protected static final String METRIC_LINK_RECEIVED = "linkReceived";
    protected static final String METRIC_LINK_PARSED = "linkParsed";
    protected static final String METRIC_LINK_VISITED = "linkVisited";
    protected static final String METRIC_LINK_TO_VISIT = "linkToVisit";
    protected static final String METRIC_DECODE_ERRORS = "decodeErrors";
    protected static final String METRIC_PARSE_ERRORS = "pageParseErrors";
    protected static final String METRIC_GROUP = "monzoCustomMetric.";

    @Autowired MeterRegistry meterRegistry;

    public void incrementLinkReceived() {
        incrementCounter(METRIC_LINK_RECEIVED, 1);
    }

    public void incrementLinkVisited() {
        incrementCounter(METRIC_LINK_VISITED, 1);
    }

    public void incrementDecodeErrors() {
        incrementCounter(METRIC_DECODE_ERRORS, 1);
    }
    public void incrementParseErrors() { incrementCounter(METRIC_PARSE_ERRORS, 1); }

    public void incrementLinkToVisit(int links) {
        incrementCounter(METRIC_LINK_TO_VISIT, links);
    }

    public void incrementLinkParsed(int links) {
        incrementCounter(METRIC_LINK_PARSED, links);
    }

    private void incrementCounter(String metricName, int value) {
        meterRegistry.counter(METRIC_GROUP+metricName).increment(value);
    }

    public Map<String, String> getMetrics() {
        return Search.in(meterRegistry)
                .name(name -> name.startsWith(METRIC_GROUP))
                .counters()
                .stream().map(counter -> Map.entry(counter.getId().toString(), String.valueOf(counter.count())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
