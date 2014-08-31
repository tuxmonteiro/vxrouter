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

public class RandomPolicy implements ILoadBalancePolicy {

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {

        if (backends!=null && !backends.isEmpty() && backends instanceof ArrayList<?>) {
            return ((ArrayList<Backend>)backends).get(getIntRandom(backends.size()));
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
