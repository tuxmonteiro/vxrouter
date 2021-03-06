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
package lbaas.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static lbaas.Constants.*;
import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.loadbalance.impl.RoundRobinPolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.shareddata.SharedData;

public class RoundRobinPolicyTest {

    private Virtualhost virtualhost;
    private int numBackends = 10;
    private Vertx vertx;
    private RequestData requestData;

    @Before
    public void setUp() throws Exception {
        vertx = mock(DefaultVertx.class);
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
        int lastBackendChosenPort = numBackends-1;
        for (int counter=0; counter<100000; counter++) {
             int backendChosenPort = virtualhost.getChoice(requestData).getPort();
             if (backendChosenPort==0) {
                 assertThat(lastBackendChosenPort).isEqualTo(numBackends-1);
             } else {
                 assertThat(backendChosenPort).isEqualTo(lastBackendChosenPort+1);
             }
             lastBackendChosenPort = backendChosenPort;
        }
    }

}
