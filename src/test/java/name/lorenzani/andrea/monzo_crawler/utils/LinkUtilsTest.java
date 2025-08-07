package name.lorenzani.andrea.monzo_crawler.utils;

import name.lorenzani.andrea.monzo_crawler.services.MetricService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LinkUtilsTest {

    @Mock MetricService metricService;
    @InjectMocks LinkUtils underTest;

    @Test
    public void testHappyPath() {
        assertEquals("http://monzo.com", underTest.normalizeLink("http://monzo.com"));
    }

    @Test
    public void testURINormalization() {
        assertEquals("http://monzo.com/path", underTest.normalizeLink("http://monzo.com/subpath/../path"));
    }

    @Test
    public void testNoInPageReference() {
        assertEquals("http://monzo.com/path", underTest.normalizeLink("http://monzo.com/subpath/../path#AnImportantParagraph"));
    }

    /*@Test
    public void testNoFinalSlash() {
        assertEquals("http://monzo.com/path", underTest.normalizeLink("http://monzo.com/subpath/../path/#notRelevant"));
    }*/

    @Test
    public void testNormalizeSpaces() {
        assertEquals("http://monzo.com/%20path", underTest.normalizeLink("http://monzo.com/subpath/../ path"));
    }

    @Test
    public void testDynamicPages() {
        assertEquals("http://monzo.com/path?key=value", underTest.normalizeLink("http://monzo.com/subpath/../path?key=value"));
        assertEquals("http://monzo.com/path/?key=value", underTest.normalizeLink("http://monzo.com/subpath/../path/?key=value"));
    }

    @Test
    public void testMalformed() {
        assertEquals("noschemanocry:&&/%", underTest.normalizeLink("noschemanocry:&&/%"));
        verify(metricService).incrementDecodeErrors();
    }

}