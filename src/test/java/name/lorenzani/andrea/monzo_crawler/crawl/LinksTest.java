package name.lorenzani.andrea.monzo_crawler.crawl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinksTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            http://monzo.com                                              | http://monzo.com/
            http://monzo.com/                                             | http://monzo.com/
            http://monzo.com/subpath/../path                              | http://monzo.com/path
            http://monzo.com/subpath/../path#AnImportantParagraph         | http://monzo.com/path
            http://monzo.com/subpath/../ path                             | http://monzo.com/%20path
            http://monzo.com/subpath/../path?key=value                    | http://monzo.com/path?key=value
            http://monzo.com/subpath/../path/?key=value                   | http://monzo.com/path/?key=value
            HTTPS://Monzo.COM/About                                       | https://monzo.com/About
            http://monzo.com:8080/a                                       | http://monzo.com:8080/a
            http://user:secret@monzo.com/a                                | http://monzo.com/a
            http://monzo.com?q=1                                          | http://monzo.com/?q=1
            http://monzo.com/#top                                         | http://monzo.com/
              http://monzo.com/a                                          | http://monzo.com/a
            """)
    void normalisesTheUrls(String raw, String expected) {
        assertEquals(Optional.of(expected), Links.normalize(raw));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "mailto:someone@monzo.com", "tel:+441234", "javascript:void(0)", "ftp://monzo.com/file",
            "/relative/path", "monzo.com", "noschemanocry:&&/%", "http://", "https:///path", "#top"})
    void rejectsWhatIsNotAnAbsoluteWebUrl(String raw) {
        assertEquals(Optional.empty(), Links.normalize(raw));
    }

    @Test
    void theHostIsLowerCase() {
        assertEquals("monzo.com", Links.host("https://monzo.com/path"));
        assertEquals("community.monzo.com", Links.host(Links.normalize("https://Community.Monzo.com:8443/x").orElseThrow()));
    }
}
