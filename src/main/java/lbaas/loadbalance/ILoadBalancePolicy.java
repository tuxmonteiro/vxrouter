/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance;

import java.util.Collection;

import lbaas.Client;
import lbaas.RequestData;

public interface ILoadBalancePolicy {

    public Client getChoice(final Collection<Client> clients, final RequestData requestData);

    public boolean isDefault();

    @Override
    public String toString();

}
