/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static lbaas.Constants.*;
import lbaas.Backend;
import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.list.UniqueArrayList;
import lbaas.loadbalance.impl.LeastConnPolicy;

import org.junit.Before;
import org.junit.Test;

public class LeastConnPolicyTest {

    private Virtualhost virtualhost;
    private int numBackends = 10;

    @Before
    public void setUp() throws Exception {

        virtualhost = new Virtualhost("test.localdomain", null);
        virtualhost.putString(loadBalancePolicyFieldName, LeastConnPolicy.class.getSimpleName());

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
            Backend backend = virtualhost.getBackends(true).get(x);
            for (int c = 1; c <= x+1; c++) {
                backend.connect("0", String.format("%s", c));
            }
        }
    }

    @Test
    public void leastConnection() {

        Backend backendWithLeastConn = virtualhost.getChoice(new RequestData());
        int numConnectionsInBackendWithLeastConn = backendWithLeastConn.getActiveConnections();

        UniqueArrayList<Backend> backends = virtualhost.getBackends(true);
        for (Backend backendSample: backends) {

            int numConnectionsInBackendSample = backendSample.getActiveConnections();
            System.out.println(String.format("%s", backendSample.getActiveConnections()));
            if (backendSample!=backendWithLeastConn) {
                assertThat(numConnectionsInBackendWithLeastConn)
                            .isLessThan(numConnectionsInBackendSample);

            }

        }

    }

}
