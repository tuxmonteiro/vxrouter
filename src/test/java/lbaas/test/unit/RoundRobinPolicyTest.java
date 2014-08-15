/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static lbaas.Constants.*;

import lbaas.Backend;
import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.list.UniqueArrayList;
import lbaas.loadbalance.impl.RoundRobinPolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.shareddata.SharedData;

public class RoundRobinPolicyTest {

    private Virtualhost virtualhost;
    private UniqueArrayList<Backend> backends;
    private int numBackends = 10;
    private Vertx vertx;
    private RequestData requestData;

    @Before
    public void setUp() throws Exception {
        vertx = mock(Vertx.class);
        SharedData sharedData = new SharedData();
        when(vertx.sharedData()).thenReturn(sharedData);

        virtualhost = new Virtualhost("test.localdomain", vertx);
        virtualhost.putString(loadBalancePolicyFieldName, RoundRobinPolicy.class.getSimpleName());

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
        }

        MultiMap headers = new CaseInsensitiveMultiMap();
        headers.add("Host", "test.localdomain");

        requestData = new RequestData(headers, null, null, "0.0.0.0", "0");
    }

    @Test
    public void backendsChosenInSequence() {
        int lastBackendChosenPort = 0;
        for (int counter=0; counter<1000; counter++) {

            // Idea: Compare backendChosenPort with lastBackendChosenPort, except on limit (counter MOD numBackends)

            // TODO: For now, I get NullPointerException exception at:
            // int backendChosenPort = virtualhost.getChoice(requestData).getPort();
        }
        fail("Not implemented");
    }

}
