/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import static lbaas.Constants.transientStateFieldName;

import java.util.Collection;
import java.util.ArrayList;

import lbaas.Backend;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;

public class RandomPolicy implements ILoadBalancePolicy {

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {
        requestData.getProperties().putBoolean(transientStateFieldName, false);

        if (backends!=null && !backends.isEmpty() && backends instanceof ArrayList<?>) {
            return ((ArrayList<Backend>)backends).get(getIntRandom(backends.size()));
        } else {
            return null;
        }
    }

    private int getIntRandom(int size) {
        return (int) (Math.random() * (size - Float.MIN_VALUE));
    }

    @Override
    public String toString() {
        return RandomPolicy.class.getSimpleName();
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}
