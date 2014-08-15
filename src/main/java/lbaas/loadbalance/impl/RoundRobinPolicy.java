/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import java.util.Collection;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;

import org.vertx.java.core.Vertx;

import lbaas.Backend;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.verticles.RouterVerticle;

public class RoundRobinPolicy implements ILoadBalancePolicy {

    private final Vertx vertx = RouterVerticle.sharedVertx;
    ConcurrentMap<String, Integer> roundRobinMap = null;

    private int size = 0;

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {

        if (roundRobinMap==null) {
            roundRobinMap = vertx.sharedData()
                    .getMap(String.format("%s_%s", this.toString(), requestData.getHeaderHost()));
        }

        size = backends.size();
        int pos = roundRobinMap.containsKey("pos") ? roundRobinMap.get("pos") : -1;
        pos = pos<size-1 ? pos+1 : 0;
        roundRobinMap.put("pos", pos);

       return ((ArrayList<Backend>)backends).get(pos);
    }

    @Override
    public String toString() {
        return RoundRobinPolicy.class.getSimpleName();
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}
