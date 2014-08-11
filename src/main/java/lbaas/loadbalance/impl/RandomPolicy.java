/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import static lbaas.Constants.transientStateFieldName;

import java.util.Collection;
import java.util.ArrayList;

import lbaas.Client;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;

public class RandomPolicy implements ILoadBalancePolicy {

    @Override
    public Client getChoice(final Collection<Client> clients, final RequestData requestData) {
        requestData.getProperties().putBoolean(transientStateFieldName, false);

        if (clients!=null && !clients.isEmpty() && clients instanceof ArrayList<?>) {
            return ((ArrayList<Client>)clients).get(getIntRandom(clients.size()));
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
