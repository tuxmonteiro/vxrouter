/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import lbaas.loadbalance.ILoadBalancePolicy;

public class DefaultLoadBalancePolicy extends RandomPolicy implements ILoadBalancePolicy {

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
