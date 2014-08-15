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
    private UniqueArrayList<Backend> backends;
    private int numBackends = 10;

    @Before
    public void setUp() throws Exception {

        virtualhost = new Virtualhost("test.localdomain", null);
        virtualhost.putString(loadBalancePolicyFieldName, LeastConnPolicy.class.getSimpleName());

        for (int x=0; x<=numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
        }

        backends = virtualhost.getBackends(true);
        int backendId = -1;
        for (final Backend backend : backends) {
            backendId++;
            for (int loop = numBackends - backendId; loop > 0; loop--) {
                backend.connect("0", String.format("%s", loop));
            }
        }
    }

    @Test
    public void leastConnection() {

        Backend backendWithLeastConn = virtualhost.getChoice(new RequestData());
        int numConnectionsInBackendWithLeastConn = backendWithLeastConn.getActiveConnections();

        for (Backend backendSample: backends) {

            int numConnectionsInBackendSample = backendSample.getActiveConnections();

            if (backendSample!=backendWithLeastConn) {
                assertThat(backendWithLeastConn).isNotEqualTo(backendSample);
                assertThat(numConnectionsInBackendWithLeastConn)
                            .isLessThan(numConnectionsInBackendSample);

            }

        }

    }

}
