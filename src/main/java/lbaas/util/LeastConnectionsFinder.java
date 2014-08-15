package lbaas.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lbaas.Backend;

public class LeastConnectionsFinder {

    private final Map<Backend, Integer> mapBackends = new HashMap<>();

    public LeastConnectionsFinder(final Collection<Backend> backends) {
        for (Backend backend : backends) {
            mapBackends.put(backend, backend.getActiveConnections());
        }
    }

    public void add(final Backend backend) {
        mapBackends.put(backend, backend.getActiveConnections());
    }

    public void addAll(final Collection<Backend> backends) {
        for (Backend backend : backends) {
            add(backend);
        }
    }

    public void update() {
        rebuild(mapBackends.keySet());
    }

    public void rebuild(final Collection<Backend> backends) {
        mapBackends.clear();
        addAll(backends);
    }

    public Backend get() {
        Backend chosen = Collections.min(mapBackends.entrySet(), new Comparator<Entry<Backend, Integer>>() {
                @Override
                public int compare(Entry<Backend, Integer> o1, Entry<Backend, Integer> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            }).getKey();
        return chosen;
    }
}
