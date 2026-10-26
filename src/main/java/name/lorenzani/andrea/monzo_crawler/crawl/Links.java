package name.lorenzani.andrea.monzo_crawler.crawl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

/** What the crawler needs to know about URLs. */
public final class Links {

    private Links() {
    }

    /**
     * A canonical form of an http or https URL, so that the same page is not visited twice: no fragment (the part after
     * {@code #}, which the server never sees), spaces encoded, {@code ..} resolved, lower case scheme and host, no
     * user and password, an empty path written as {@code /}.
     *
     * @return empty for anything that is not an absolute http(s) URL with a host (mailto:, javascript:, tel:...)
     */
    public static Optional<String> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String withoutFragment = raw.strip();
        int fragment = withoutFragment.indexOf('#');
        if (fragment >= 0) {
            withoutFragment = withoutFragment.substring(0, fragment);
        }
        try {
            URI uri = new URI(withoutFragment.replace(" ", "%20")).normalize();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null) {
                return Optional.empty();
            }
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            return Optional.of(scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                    + path
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    /** The lower case host of a URL that went through {@link #normalize}. */
    public static String host(String normalizedUrl) {
        return URI.create(normalizedUrl).getHost().toLowerCase(Locale.ROOT);
    }
}
