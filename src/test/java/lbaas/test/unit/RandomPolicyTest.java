/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.test.unit;

import static lbaas.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.loadbalance.impl.RandomPolicy;

import org.junit.Before;
import org.junit.Test;

public class RandomPolicyTest {

    Virtualhost virtualhost;

    @Before
    public void setUp() throws Exception {
        virtualhost = new Virtualhost("test.localdomain", null);

        virtualhost.putString(loadBalancePolicyFieldName, RandomPolicy.class.getSimpleName());

        int numClients = 10;
        for (int x=0; x<numClients; x++) {
            virtualhost.addClient(String.format("0:%s", x), true);
        }
    }

    @Test
    public void checkUniformDistribution() {
        long sum = 0;
        double percentMarginOfError = 0.01;
        long samples = 100000L;
        int numEndpoints = virtualhost.getClients(true).size();

        long initialTime = System.currentTimeMillis();
        for (int x=0; x<samples; x++) {
            RequestData requestData = new RequestData("127.0.0.1", null);
            sum += virtualhost.getChoice(requestData).getPort();
        }
        long finishTime = System.currentTimeMillis();

        double result = (numEndpoints*(numEndpoints-1)/2.0) * (1.0*samples/numEndpoints);

        System.out.println(String.format("TestRandomPolicy.checkUniformDistribution: %d samples. Total time (ms): %d. NonUniformDistRatio%%: %.10f",
                    samples, finishTime-initialTime, Math.abs(100.0*(result-sum)/result)));

        double topLimit = sum*(1.0+percentMarginOfError);
        double bottomLimit = sum*(1.0-percentMarginOfError);

        assertThat(result).isGreaterThanOrEqualTo(bottomLimit)
                          .isLessThanOrEqualTo(topLimit);
    }

}
