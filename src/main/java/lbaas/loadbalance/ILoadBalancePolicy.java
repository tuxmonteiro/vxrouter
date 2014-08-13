/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance;

import java.util.Collection;

import lbaas.Backend;
import lbaas.RequestData;

public interface ILoadBalancePolicy {

    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData);

    public boolean isDefault();

    @Override
    public String toString();

}
