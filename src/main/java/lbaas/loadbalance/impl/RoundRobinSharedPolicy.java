/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import java.util.Collection;
import java.util.ArrayList;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.shareddata.SharedData;

import lbaas.Backend;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.verticles.RouterVerticle;

// NOT TESTED - Use at your own risk
public class RoundRobinSharedPolicy implements ILoadBalancePolicy {

    private final Vertx vertx = RouterVerticle.sharedVertx;
    private final SharedData shared = vertx.sharedData();

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {

        String mapKey = String.format("%s_%s", this.toString(), requestData.getHeaderHost());
        int size = backends.size();
        Integer last = (Integer) shared.getMap(mapKey).get("pos");
        Integer pos = last==null ? 0 : (last+1==size ? 0 : last+1);
        shared.getMap(mapKey).put("pos", pos);

       return ((ArrayList<Backend>)backends).get(pos);
    }

    @Override
    public String toString() {
        return RoundRobinSharedPolicy.class.getSimpleName();
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}
