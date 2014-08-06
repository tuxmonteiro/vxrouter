package lbaas.unitTest;

import static org.assertj.core.api.Assertions.*;
import static lbaas.unitTest.assertj.custom.VirtualHostAssert.*;
import static org.mockito.Mockito.*;
import lbaas.Client;
import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.loadbalance.ILoadBalancePolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class TestVirtualhost {

    String virtualhostName;
    Vertx vertx;
    Virtualhost virtualhost;
    String endpoint;
    RequestData requestData;

    @Before
    public void setUp(){
        virtualhostName = "virtualhost1";
        vertx = null;
        requestData = new RequestData(null);
        virtualhost = new Virtualhost(virtualhostName, vertx);
        endpoint = "0.0.0.0:0";
    }

    @Test
    public void insertNewClientInSet() {
        boolean endPointOk = true;

        boolean notFail = virtualhost.addClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(1, endPointOk);
    }

    @Test
    public void insertNewBadClientInSet() {
        boolean endPointOk = false;
        boolean notExist = virtualhost.addClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionOk(notExist).hasSize(1, endPointOk);
    }

    @Test
    public void insertDuplicatedClientInSet() {
        boolean endPointOk = true;

        virtualhost.addClient(endpoint, endPointOk);
        boolean notFail = virtualhost.addClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(1, endPointOk);

    }

    @Test
    public void insertDuplicatedBadClientInSet() {
        boolean endPointOk = false;

        virtualhost.addClient(endpoint, endPointOk);
        boolean notFail = virtualhost.addClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(1, endPointOk);

    }

    @Test
    public void removeExistingClientInSet() {
        boolean endPointOk = true;

        virtualhost.addClient(endpoint, endPointOk);
        boolean notFail = virtualhost.removeClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(0, endPointOk);
    }

    @Test
    public void removeExistingBadClientInSet() {
        boolean endPointOk = false;

        virtualhost.addClient(endpoint, endPointOk);
        boolean notFail = virtualhost.removeClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(0, endPointOk);
    }

    @Test
    public void removeAbsentClientInSet() {
        boolean endPointOk = true;

        boolean notFail = virtualhost.removeClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(0, endPointOk);

    }

    @Test
    public void removeAbsentBadClientInSet() {
        boolean endPointOk = false;

        boolean notFail = virtualhost.removeClient(endpoint, endPointOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(0, endPointOk);

    }

    @Test
    public void getChoiceNewConnectInterface() {
        boolean endPointOk = true;
        ILoadBalancePolicy loadBalancePolicy = mock(ILoadBalancePolicy.class);
        virtualhost.setConnectPolicy(loadBalancePolicy);
        virtualhost.addClient(endpoint, endPointOk);

        when(loadBalancePolicy.getChoice(anyCollectionOf(Client.class), (RequestData) any()))
            .thenReturn(virtualhost.getClients(endPointOk).iterator().next());

        String endpointHost = virtualhost.getChoice().getHost();
        Integer endpointPort = virtualhost.getChoice().getPort();

        assertThat(endpointHost).isEqualToIgnoringCase(endpoint.split(":")[0]);
        assertThat(endpointPort).isEqualTo(Integer.parseInt(endpoint.split(":")[1]));
    }

    @Test
    public void getChoicePersistenceInterface() {
        boolean endPointOk = true;
        ILoadBalancePolicy loadBalancePolicy = mock(ILoadBalancePolicy.class);
        virtualhost.setPersistencePolicy(loadBalancePolicy);
        virtualhost.addClient(endpoint, endPointOk);

        when(loadBalancePolicy.getChoice(anyCollectionOf(Client.class), (RequestData) any()))
            .thenReturn(virtualhost.getClients(endPointOk).iterator().next());

        String endpointHost = virtualhost.getChoice(false).getHost();
        Integer endpointPort = virtualhost.getChoice(false).getPort();

        assertThat(endpointHost).isEqualToIgnoringCase(endpoint.split(":")[0]);
        assertThat(endpointPort).isEqualTo(Integer.parseInt(endpoint.split(":")[1]));
    }

    @Test
    public void getLoadBalancePolicyClassFound() {
        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy("RandomPolicy");
        assertThat(loadBalance.toString()).isEqualTo("RandomPolicy");
    }

    @Test
    public void getLoadBalancePolicyClassNotFound() {
        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy("ClassNotExist");
        assertThat(loadBalance).isNull();
    }

    @Test
    public void getLoadBalancePolicyNewConnection() {
        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy("RandomPolicy");

        virtualhost.setConnectPolicy(loadBalance);
        virtualhost.addClient(endpoint, true);

        assertThat(virtualhost.getChoice(true)).isEqualTo(new Client(endpoint, null));
    }

    @Test
    public void getLoadBalancePolicyPersistence() {
        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy("RandomPolicy");

        virtualhost.setPersistencePolicy(loadBalance);
        virtualhost.addClient(endpoint, true);

        assertThat(virtualhost.getChoice(false).toString()).isEqualTo(endpoint);
    }

    @Test
    public void defineLoadBalancePolicy() {
        JsonObject properties = new JsonObject();
        String loadBalancePolicyStr = "RandomPolicy";
        properties.putString("loadBalancePolicy", loadBalancePolicyStr);

        virtualhost.setProperties(properties);
        ILoadBalancePolicy loadBalancePolicy = virtualhost.getLoadBalancePolicy();

        assertThat(loadBalancePolicy.toString()).isEqualTo(loadBalancePolicyStr);
    }
}
