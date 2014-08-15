/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import java.util.Collection;
import java.util.ArrayList;
import lbaas.Backend;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;

public class RoundRobinPolicy implements ILoadBalancePolicy {

    private int pos = -1;

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {

        int size = backends.size();
        pos = pos+1>=size ? 0 : pos+1;

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
