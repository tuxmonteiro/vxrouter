/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.loadbalance.impl;

import static lbaas.Constants.*;

import java.util.Collection;

import org.vertx.java.core.json.JsonObject;

import lbaas.Client;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.util.ConsistentHash;
import lbaas.util.HashAlgorithm;

public class HashPolicy implements ILoadBalancePolicy {

    private ConsistentHash<Client> consistentHash = null;
    private String                 lastHashType   = null;
    private long                   lastReset      = System.currentTimeMillis();

    @Override
    public Client getChoice(final Collection<Client> clients, final RequestData requestData) {

        String sourceIp = requestData.getRemoteAddress();
        JsonObject properties = requestData.getProperties();
        String hashType = properties.getString(hashAlgorithmFieldName);
        long timeout = properties.getLong(cacheTimeOutFieldName, 0L);
        boolean transientState = properties.getBoolean(transientStateFieldName, false);

        long now = System.currentTimeMillis();
        int numberOfReplicas = 1;

        if (lastHashType == null || consistentHash == null) {
            lastHashType = hashType;
            transientState = false;
            lastReset = now;
            consistentHash = new ConsistentHash<Client>(
                    new HashAlgorithm(hashType), numberOfReplicas, clients);
        }

        if (!lastHashType.equals(hashType)) {
            consistentHash.rebuild(new HashAlgorithm(hashType), numberOfReplicas, clients);
            lastHashType = hashType;
            lastReset = now;
        } else if (transientState) {
            consistentHash.rebuild(null, null, clients);
            lastReset = now;
        } else if ((lastReset + timeout) < now) {
            consistentHash.resetCache();
            lastReset = now;
        }

        properties.putBoolean(transientStateFieldName, false);
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
