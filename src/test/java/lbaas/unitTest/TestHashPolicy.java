/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static lbaas.Constants.*;
import static lbaas.util.HashAlgorithm.HashType;

import java.util.EnumSet;
import java.util.Set;

import lbaas.Client;
import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.loadbalance.impl.HashPolicy;
import lbaas.util.HashAlgorithm;

import org.junit.Before;
import org.junit.Test;

public class TestHashPolicy {

    Virtualhost virtualhost;
    int numEndpoints;

    @Before
    public void setUp() throws Exception {
        virtualhost = new Virtualhost("test.localdomain", null);
        virtualhost.putString(loadBalancePolicyFieldName, HashPolicy.class.getSimpleName());
        virtualhost.putNumber(cacheTimeOutFieldName, 60*1000L);

        numEndpoints = 10;
        for (int x=0; x<numEndpoints; x++) {
            virtualhost.addClient(String.format("0:%s", x), true);
        }
    }

    @Test
    public void checkPersistentChoice() {
        long numTests = 256L*256L;

        for (int counter=0; counter<numTests; counter++) {

            RequestData requestData1 = new RequestData(Long.toString(counter), null);
            Client client1 = virtualhost.getChoice(requestData1);
            RequestData requestData2 = new RequestData(Long.toString(counter), null);
            Client client2 = virtualhost.getChoice(requestData2);
            RequestData requestData3 = new RequestData(Long.toString(counter), null);
            Client client3 = virtualhost.getChoice(requestData3);

            assertThat(client1).isEqualTo(client2);
            assertThat(client1).isEqualTo(client3);
        }
    }

    @Test
    public void checkUniformDistribution() {
        long samples = 100000L;
        int rounds = 5;
        double percentMarginOfError = 0.5;
        Set<HashType> hashs = EnumSet.allOf(HashAlgorithm.HashType.class);

        for (int round=0; round < rounds; round++) {
            System.out.println(String.format("TestHashPolicy.checkUniformDistribution - round %s: %d samples", round+1, samples));

            for (HashType hash: hashs) {

                long sum = 0L;
                long initialTime = System.currentTimeMillis();
                for (int counter=0; counter<samples; counter++) {
                    RequestData requestData = new RequestData(Long.toString(counter), null);
                    virtualhost.putString(hashAlgorithmFieldName, hash.toString());
                    sum += virtualhost.getChoice(requestData).getPort();
                }
                long finishTime = System.currentTimeMillis();

                double result = (numEndpoints*(numEndpoints-1)/2.0) * (samples/numEndpoints);

                System.out.println(String.format("-> TestHashPolicy.checkUniformDistribution (%s): Time spent (ms): %d. NonUniformDistRatio (smaller is better): %.4f%%",
                        hash, finishTime-initialTime, Math.abs(100.0*(result-sum)/result)));

                double topLimit = sum*(1.0+percentMarginOfError);
                double bottomLimit = sum*(1.0-percentMarginOfError);

                assertThat(result).isGreaterThanOrEqualTo(bottomLimit)
                                  .isLessThanOrEqualTo(topLimit);
            }
        }
    }
}
