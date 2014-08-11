/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {

    private HashAlgorithm               hashAlgorithm;
    private int                         numberOfReplicas;
    private final SortedMap<Integer, T> circle  = new TreeMap<Integer, T>();
    private final Map<String, Integer>  mapKeys = new HashMap<>();

    public ConsistentHash(HashAlgorithm hashAlgorithm, int numberOfReplicas,
            Collection<T> nodes) {
        this.hashAlgorithm = hashAlgorithm;
        this.numberOfReplicas = numberOfReplicas;

        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hashAlgorithm.hash(node.toString() + i), node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hashAlgorithm.hash(node.toString() + i));
        }
    }

    public T get(String key) {
        if (circle.isEmpty()) {
            return null;
        }

        int hash;
        if (!mapKeys.containsKey(key)) {
            hash = hashAlgorithm.hash(key);
            mapKeys.put(key, hash);
        }
        hash = mapKeys.get(key);

        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public void resetCache() {
        mapKeys.clear();
    }

    public void rebuild(HashAlgorithm hashAlgorithm, Integer numberOfReplicas,
            Collection<T> nodes) {
        if (hashAlgorithm!=null) {
            this.hashAlgorithm = hashAlgorithm;
        }
        if (numberOfReplicas!=null) {
            this.numberOfReplicas = numberOfReplicas;
        }

        resetCache();
        circle.clear();
        for (T node : nodes) {
            add(node);
        }
    }

}
