package name.lorenzani.andrea.monzo_crawler.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static name.lorenzani.andrea.monzo_crawler.services.MetricService.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    @Mock MeterRegistry meterRegistry;
    @InjectMocks MetricService underTest;

    @BeforeEach
    public void setup() {
        when(meterRegistry.counter(anyString())).thenReturn(Mockito.mock(Counter.class));
    }

    @Test
    public void testIncrementLinkReceived() {
        underTest.incrementLinkReceived();
        testMetrics(1,0,0,0,0,0);
    }

    @Test
    public void testIncrementLinkParsed() {
        underTest.incrementLinkParsed(1);
        testMetrics(0,1,0,0,0,0);
    }

    @Test
    public void testIncrementLinkVisited() {
        underTest.incrementLinkVisited();
        testMetrics(0,0,1,0,0,0);
    }

    @Test
    public void testIncrementLinkToVisit() {
        underTest.incrementLinkToVisit(1);
        testMetrics(0,0,0,1,0,0);
    }

    @Test
    public void testIncrementDecodeErrors() {
        underTest.incrementDecodeErrors();
        testMetrics(0,0,0,0,1,0);
    }

    @Test
    public void testIncrementParseErrors() {
        underTest.incrementParseErrors();
        testMetrics(0,0,0,0,0,1);
    }

    private void testMetrics(int linkReceived, int linkParsed, int linkVisited, int linkToVisit, int decodeErrors, int parseErrors) {
        verify(meterRegistry, times(linkReceived)).counter(METRIC_GROUP+METRIC_LINK_RECEIVED);
        verify(meterRegistry, times(linkParsed)).counter(METRIC_GROUP+METRIC_LINK_PARSED);
        verify(meterRegistry, times(linkVisited)).counter(METRIC_GROUP+METRIC_LINK_VISITED);
        verify(meterRegistry, times(linkToVisit)).counter(METRIC_GROUP+METRIC_LINK_TO_VISIT);
        verify(meterRegistry, times(decodeErrors)).counter(METRIC_GROUP+METRIC_DECODE_ERRORS);
        verify(meterRegistry, times(parseErrors)).counter(METRIC_GROUP+METRIC_PARSE_ERRORS);
    }

}