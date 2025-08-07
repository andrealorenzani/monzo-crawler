package name.lorenzani.andrea.monzo_crawler.services;

import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Set;

@Service
public class CacheService {
    protected HashMap<String, String> cache = new HashMap<>();

    public String addVisited(String url) {
        return put("visited-"+url, url);
    }

    public boolean isVisited(String url) {
        return contains("visited-"+url);
    }

    public int numPagesVisited() {
        return cache.size();
    }
    public Set<String> pagesVisited() { return cache.keySet(); }

    private boolean contains(String key) {
        return cache.containsKey(key);
    }

    private synchronized String put(String key, String value) {
        return cache.put(key, value);
    }
    public synchronized void invalidate(String url) throws URISyntaxException {
        // This could invalidate only the domain of the request or we may just set a TTL on the entry
        cache.clear();
    }

}
