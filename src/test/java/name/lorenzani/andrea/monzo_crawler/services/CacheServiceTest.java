package name.lorenzani.andrea.monzo_crawler.services;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheServiceTest {

    private CacheService underTest;

    @Test
    public void testAddVisited() {
        underTest = new CacheService();
        String fakeUrl = "thisIsAnURL";

        underTest.addVisited(fakeUrl);
        assertTrue(underTest.isVisited(fakeUrl));
        assertEquals(1, underTest.numPagesVisited());
    }

    @Test
    public void testPageVisited() {
        underTest = new CacheService();
        String fakeUrl = "thisIsAnURL";

        underTest.addVisited(fakeUrl);
        assertEquals(1, underTest.numPagesVisited());
        assertEquals(Set.of("visited-"+fakeUrl), underTest.pagesVisited());
    }

    @Test
    public void testInvalidate() throws URISyntaxException {
        underTest = new CacheService();
        String fakeUrl = "thisIsAnURL";
        String fakeUrl2 = "thisCouldBeADifferentDomain"; // Please note that it does not invalidate by domain, but it should

        underTest.addVisited(fakeUrl);
        underTest.addVisited(fakeUrl2);
        assertEquals(2, underTest.numPagesVisited());
        underTest.invalidate(fakeUrl);
        assertEquals(0, underTest.numPagesVisited());
    }

}