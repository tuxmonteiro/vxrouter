/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import lbaas.Client;
import lbaas.RequestData;
import lbaas.list.UniqueArrayList;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.loadbalance.impl.HashPolicy;

import org.junit.Before;
import org.junit.Test;

public class TestHashPolicy {

    UniqueArrayList<Client> clients = new UniqueArrayList<>();
    ILoadBalancePolicy hashPolicy = new HashPolicy();
    RequestData requestData = new RequestData(null);

    @Before
    public void setUp() throws Exception {
        requestData.setRemoteAddress("127.0.0.1");
        requestData.setRemotePort("80");
        clients.clear();

        for (int x=0; x<100; x++) {
            clients.add(new Client(String.format("0:%s", x), null));
        }
    }

    @Test
    public void checkAlgorithmChoice() {
        Client clientChosen = hashPolicy.getChoice(clients, requestData);
        Client sameclientChosen = hashPolicy.getChoice(clients, requestData);

        assertThat(clientChosen).isEqualTo(sameclientChosen);
    }

    @Test
    public void checkAlgorithmWrongChoice() {
        Client clientChosen = hashPolicy.getChoice(clients, requestData);

        requestData.setRemoteAddress("127.0.0.2");
        Client sameclientChosen = hashPolicy.getChoice(clients, requestData);

        assertThat(clientChosen).isNotEqualTo(sameclientChosen);
    }

}
