/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import static lbaas.Constants.*;

import java.util.Collection;

import org.vertx.java.core.json.JsonObject;

import lbaas.Backend;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.util.LeastConnectionsFinder;

public class LeastConnPolicy implements ILoadBalancePolicy {

    private LeastConnectionsFinder leastConnectionsFinder = null;
    private long lastReset = System.currentTimeMillis();

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {

        JsonObject properties = requestData.getProperties();
        long timeout = properties.getLong(cacheTimeOutFieldName, 1000L);
        boolean transientState = properties.getBoolean(transientStateFieldName, false);

        long now = System.currentTimeMillis();

        if (leastConnectionsFinder == null) {
            transientState = false;
            lastReset = now;
            leastConnectionsFinder = new LeastConnectionsFinder(backends);
        }

        if (transientState) {
            leastConnectionsFinder.rebuild(backends);
            lastReset = now;
        } else if ((lastReset + timeout) < now) {
            leastConnectionsFinder.update();
            lastReset = now;
        }

        return leastConnectionsFinder.get();
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public String toString() {
        return LeastConnPolicy.class.getSimpleName();
    }

}
