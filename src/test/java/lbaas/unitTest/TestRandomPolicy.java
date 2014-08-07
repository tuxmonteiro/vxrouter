package lbaas.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import lbaas.Client;
import lbaas.list.UniqueArrayList;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.loadbalance.impl.RandomPolicy;

import org.junit.Before;
import org.junit.Test;

public class TestRandomPolicy {

    UniqueArrayList<Client> clients = new UniqueArrayList<>();
    ILoadBalancePolicy randomPolicy = new RandomPolicy();

    @Before
    public void setUp() throws Exception {
        clients.clear();
    }

    @Test
    public void checkAlgorithmChoice() {
        int sum = 0;
        int numInterations = 10000;
        double percentMarginOfError = 0.05;

        clients.add(new Client("0:0", null));
        clients.add(new Client("0:1", null));
        clients.add(new Client("0:2", null));
        clients.add(new Client("0:3", null));
        clients.add(new Client("0:4", null));
        int numEndpoints = clients.size();

        for (int x=0; x<numInterations; x++) {
            sum += randomPolicy.getChoice(clients, null).getPort();
        }
        int result = (numEndpoints*(numEndpoints-1)/2) * (numInterations/numEndpoints);

        assertThat(result).isGreaterThanOrEqualTo((int) (sum*(1.0-percentMarginOfError)))
                          .isLessThanOrEqualTo((int) (sum*(1.0+percentMarginOfError)));
    }

}
