/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.util;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {

 private final HashAlgorithm hashAlgorithm;
 private final int numberOfReplicas;
 private final SortedMap<Integer, T> circle = new TreeMap<Integer, T>();

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
   int hash = hashAlgorithm.hash(key);
   if (!circle.containsKey(hash)) {
     SortedMap<Integer, T> tailMap = circle.tailMap(hash);
     hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
   }
   return circle.get(hash);
 }

}