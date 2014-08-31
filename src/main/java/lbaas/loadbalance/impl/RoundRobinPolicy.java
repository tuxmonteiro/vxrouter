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

import java.util.Collection;
import java.util.ArrayList;

import lbaas.core.Backend;
import lbaas.core.RequestData;
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
