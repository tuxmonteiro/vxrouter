/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.test.unit;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;

import lbaas.list.UniqueArrayList;

import org.junit.Test;

public class UniqueArrayListTest {

    @Test
    public void constructorWithArrayList() {
        ArrayList<Integer> integers = new ArrayList<>();

        for (int x=0;x<100;x++) {
            integers.add(x);
        }
        integers.add(10);
        integers.add(20);
        integers.add(30);
        integers.add(40);
        integers.add(50);

        ArrayList<Integer> uniqIntegers = new UniqueArrayList<>(integers);

        assertThat(integers).hasSize(105);
        assertThat(uniqIntegers).hasSize(100);

    }

    @Test
    public void addNew() {
        ArrayList<Integer> uniqIntegers = new UniqueArrayList<>();

        assertThat(uniqIntegers.add(0)).isTrue();
    }

    @Test
    public void addDuplicated() {
        ArrayList<Integer> uniqIntegers = new UniqueArrayList<>();

        uniqIntegers.add(0);

        assertThat(uniqIntegers.add(0)).isFalse();
    }
}
