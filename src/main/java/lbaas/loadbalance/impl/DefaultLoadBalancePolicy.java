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
