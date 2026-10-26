package name.lorenzani.andrea.monzo_crawler.fetch;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkExtractorTest {

    private final LinkExtractor underTest = new LinkExtractor();

    private static String html(String body) {
        return "<html><head><title>The in memory homepage</title></head><body>" + body + "</body></html>";
    }

    @Test
    void extractsTheLinksInOrder() {
        var document = Jsoup.parse(html("<a href=\"http://link1.com\">1</a><a href=\"http://link2.net\">2</a>"));

        assertEquals(List.of("http://link1.com", "http://link2.net"), underTest.extract(document));
    }

    @Test
    void noLinks() {
        assertEquals(List.of(), underTest.extract(Jsoup.parse(html("<p>nothing to see</p>"))));
    }

    @Test
    void resolvesRelativeLinksAgainstThePage() {
        var document = Jsoup.parse(html("<a href=\"/about\">a</a><a href=\"team\">t</a><a href=\"../up\">u</a>"),
                "https://monzo.com/company/");

        assertEquals(List.of("https://monzo.com/about", "https://monzo.com/company/team", "https://monzo.com/up"),
                underTest.extract(document));
    }

    @Test
    void ignoresAnchorsWithoutHrefAndKeepsTheOtherSchemes() {
        var document = Jsoup.parse(html("<a name=\"x\">no href</a><a href=\"mailto:a@b.c\">mail</a><a href=\"\">self</a>"),
                "https://monzo.com/page");

        assertEquals(List.of("mailto:a@b.c", "https://monzo.com/page"), underTest.extract(document));
    }
}
