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

import static lbaas.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.loadbalance.impl.RandomPolicy;

import org.junit.Before;
import org.junit.Test;

public class RandomPolicyTest {

    Virtualhost virtualhost;
    int numBackends = 10;

    @Before
    public void setUp() throws Exception {
        virtualhost = new Virtualhost("test.localdomain", null);

        virtualhost.putString(loadBalancePolicyFieldName, RandomPolicy.class.getSimpleName());

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
        }
    }

    @Test
    public void checkUniformDistribution() {
        long sum = 0;
        double percentMarginOfError = 0.01;
        long samples = 100000L;

        long initialTime = System.currentTimeMillis();
        for (int x=0; x<samples; x++) {
            RequestData requestData = new RequestData("127.0.0.1", null);
            sum += virtualhost.getChoice(requestData).getPort();
        }
        long finishTime = System.currentTimeMillis();

        double result = (numBackends*(numBackends-1)/2.0) * (1.0*samples/numBackends);

        System.out.println(String.format("TestRandomPolicy.checkUniformDistribution: %d samples. Total time (ms): %d. NonUniformDistRatio%%: %.10f",
                    samples, finishTime-initialTime, Math.abs(100.0*(result-sum)/result)));

        double topLimit = sum*(1.0+percentMarginOfError);
        double bottomLimit = sum*(1.0-percentMarginOfError);

        assertThat(result).isGreaterThanOrEqualTo(bottomLimit)
                          .isLessThanOrEqualTo(topLimit);
    }

}
