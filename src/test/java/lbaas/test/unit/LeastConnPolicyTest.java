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

import static org.assertj.core.api.Assertions.assertThat;
import static org.vertx.testtools.VertxAssert.testComplete;
import static lbaas.Constants.*;
import lbaas.Backend;
import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.list.UniqueArrayList;
import lbaas.loadbalance.impl.LeastConnPolicy;

import org.junit.Test;
import org.vertx.testtools.TestVerticle;

public class LeastConnPolicyTest extends TestVerticle {

    private Virtualhost virtualhost;
    private int numBackends = 10;

    @Test
    public void leastConnection() {

        virtualhost = new Virtualhost("test.localdomain", vertx);
        virtualhost.putString(loadBalancePolicyFieldName, LeastConnPolicy.class.getSimpleName());

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
            Backend backend = virtualhost.getBackends(true).get(x);
            for (int c = 1; c <= x+1; c++) {
                backend.connect("0", String.format("%s", c));
            }
        }

        for (int c=1 ; c<=1000; c++) {

            Backend backendWithLeastConn = virtualhost.getChoice(new RequestData());
            int numConnectionsInBackendWithLeastConn = backendWithLeastConn.getSessionController().getActiveConnections();

            UniqueArrayList<Backend> backends = virtualhost.getBackends(true);
            for (Backend backendSample: backends) {

                int numConnectionsInBackendSample = backendSample.getSessionController().getActiveConnections();
                if (backendSample!=backendWithLeastConn) {
                    assertThat(numConnectionsInBackendWithLeastConn).isEqualTo(1);
                    assertThat(numConnectionsInBackendWithLeastConn)
                                .isLessThan(numConnectionsInBackendSample);

                }

            }
        }

        testComplete();


    }

}
