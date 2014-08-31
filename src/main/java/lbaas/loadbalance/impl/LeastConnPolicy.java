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
package lbaas.loadbalance.impl;

import static lbaas.core.Constants.*;

import java.util.Collection;

import org.vertx.java.core.json.JsonObject;

import lbaas.core.Backend;
import lbaas.core.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;

public class LeastConnPolicy implements ILoadBalancePolicy {

    private LeastConnectionsFinder leastConnectionsFinder = null;
    private long lastReset = System.currentTimeMillis();

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {

        JsonObject properties = requestData.getProperties();
        long timeout = properties.getLong(cacheTimeOutFieldName, 2000L);
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
