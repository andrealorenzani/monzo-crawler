package name.lorenzani.andrea.monzo_crawler.visited;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryVisitedStore implements VisitedStore {

    private final ConcurrentHashMap<String, Set<String>> visitedByHost = new ConcurrentHashMap<>();

    @Override
    public boolean markVisited(String host, String url) {
        return visitedByHost.computeIfAbsent(host, h -> ConcurrentHashMap.newKeySet()).add(url);
    }

    @Override
    public boolean isVisited(String host, String url) {
        return visitedByHost.getOrDefault(host, Set.of()).contains(url);
    }

    @Override
    public int count(String host) {
        return visitedByHost.getOrDefault(host, Set.of()).size();
    }

    @Override
    public void forget(String host) {
        visitedByHost.remove(host);
    }
}
