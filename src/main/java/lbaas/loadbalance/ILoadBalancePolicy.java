package lbaas.loadbalance;

import java.util.Collection;

import lbaas.Client;
import lbaas.RequestData;

public interface ILoadBalancePolicy {

    public Client getChoice(Collection<Client> clients, RequestData requestData);

    @Override
    public String toString();

}
