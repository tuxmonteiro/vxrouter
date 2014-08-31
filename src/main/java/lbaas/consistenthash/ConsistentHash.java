/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package lbaas.consistenthash;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {

    private HashAlgorithm               hashAlgorithm;
    private int                         numberOfReplicas;
    private final SortedMap<Integer, T> circle  = new TreeMap<Integer, T>();

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

        int hash= hashAlgorithm.hash(key);

        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public void rebuild(HashAlgorithm hashAlgorithm, Integer numberOfReplicas,
            Collection<T> nodes) {
        if (hashAlgorithm!=null) {
            this.hashAlgorithm = hashAlgorithm;
        }
        if (numberOfReplicas!=null) {
            this.numberOfReplicas = numberOfReplicas;
        }

        circle.clear();
        for (T node : nodes) {
            add(node);
        }
    }

}
