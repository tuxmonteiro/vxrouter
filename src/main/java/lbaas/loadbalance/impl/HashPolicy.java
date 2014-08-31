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

import static lbaas.core.Constants.*;

import java.util.Collection;

import org.vertx.java.core.json.JsonObject;

import lbaas.consistenthash.ConsistentHash;
import lbaas.consistenthash.HashAlgorithm;
import lbaas.core.Backend;
import lbaas.core.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;

public class HashPolicy implements ILoadBalancePolicy {

    private ConsistentHash<Backend> consistentHash = null;
    private String                 lastHashType   = null;

    @Override
    public Backend getChoice(final Collection<Backend> backends, final RequestData requestData) {

        String sourceIp = requestData.getRemoteAddress();
        JsonObject properties = requestData.getProperties();
        String hashType = properties.getString(hashAlgorithmFieldName, defaultHashAlgorithm);
        boolean transientState = properties.getBoolean(transientStateFieldName, false);

        int numberOfReplicas = 1;

        if (lastHashType == null || consistentHash == null) {
            lastHashType = hashType;
            transientState = false;
            consistentHash = new ConsistentHash<Backend>(
                    new HashAlgorithm(hashType), numberOfReplicas, backends);
        }

        if (!lastHashType.equals(hashType)) {
            consistentHash.rebuild(new HashAlgorithm(hashType), numberOfReplicas, backends);
            lastHashType = hashType;
        } else if (transientState) {
            consistentHash.rebuild(null, null, backends);
        }

        return consistentHash.get(sourceIp);
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public String toString() {
        return HashPolicy.class.getSimpleName();
    }

}
