/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import static lbaas.Constants.*;

import java.util.Collection;

import org.vertx.java.core.json.JsonObject;

import lbaas.Backend;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.util.ConsistentHash;
import lbaas.util.HashAlgorithm;

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
