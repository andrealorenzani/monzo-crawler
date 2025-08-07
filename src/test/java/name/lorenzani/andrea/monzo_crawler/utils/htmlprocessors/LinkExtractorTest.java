package name.lorenzani.andrea.monzo_crawler.utils.htmlprocessors;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
class LinkExtractorTest {

    @InjectMocks
    LinkExtractor underTest;

    @Test
    public void testHappyPath() {
        String link1 = "http://link1.com", link2 = "http://link2.net";
        List<String> urls = List.of(link1, link2);
        List<String> result =underTest.process(Jsoup.parse(generateHtml(urls)));
        assertEquals(List.of(link1, link2), result);
    }

    @Test
    public void testNoLink() {
        List<String> result =underTest.process(Jsoup.parse(generateHtml(List.of())));
        assertEquals(List.of(), result);
    }

    private String generateHtml(List<String> urls) {
        String links = urls.stream().map(url -> String.format("<a href=\"%s\">Link</a>", url)).collect(Collectors.joining("\n"));
        return String.format("""
                <html>
                    <head><title>The in memory homepage</title></head>
                    <body>
                        %s
                    </body>
                </html>
                """, links);
    }

}