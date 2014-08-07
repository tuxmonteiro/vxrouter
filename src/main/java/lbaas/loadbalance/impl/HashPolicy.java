package lbaas.loadbalance.impl;

import static lbaas.util.HashAlgorithm.HashType.MURMUR3_128;

import java.util.Collection;

import lbaas.Client;
import lbaas.RequestData;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.util.ConsistentHash;
import lbaas.util.HashAlgorithm;

public class HashPolicy implements ILoadBalancePolicy {

    @Override
    public Client getChoice(Collection<Client> clients, RequestData requestData) {
        String sourceIp = requestData.getRemoteAddress();
        int numberOfReplicas = 1;
        ConsistentHash<Client> consistentHash = new ConsistentHash<Client>(new HashAlgorithm(MURMUR3_128),
                                                                           numberOfReplicas,
                                                                           clients);
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
