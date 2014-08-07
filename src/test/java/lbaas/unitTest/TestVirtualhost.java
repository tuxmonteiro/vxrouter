package lbaas.unitTest;

import static org.assertj.core.api.Assertions.*;
import static lbaas.unitTest.assertj.custom.VirtualHostAssert.*;
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
        virtualhost.clearProperties();
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
    public void loadBalancePolicyClassFound() {
        JsonObject properties = new JsonObject();
        String loadBalancePolicyStr = "RandomPolicy";
        properties.putString(virtualhost.getLoadBalancePolicyFieldName(), loadBalancePolicyStr);

        virtualhost.setProperties(properties);
        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy();

        assertThat(loadBalance.toString()).isEqualTo(loadBalancePolicyStr);
    }

    @Test
    public void loadBalancePolicyClassNotFound() {
        JsonObject properties = new JsonObject();
        String loadBalancePolicyStr = "ClassNotExist";
        properties.putString(virtualhost.getLoadBalancePolicyFieldName(), loadBalancePolicyStr);

        virtualhost.setProperties(properties);
        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy();

        assertThat(loadBalance).isNull();
    }

    @Test
    public void getClientWithLoadBalancePolicy() {
        JsonObject properties = new JsonObject();
        String loadBalancePolicyStr = "RandomPolicy";
        properties.putString(virtualhost.getLoadBalancePolicyFieldName(), loadBalancePolicyStr);

        virtualhost.setProperties(properties);
        virtualhost.addClient(endpoint, true);

        assertThat(virtualhost.getChoice().toString()).isEqualTo(endpoint);
    }

    @Test
    public void getClientWithPersistencePolicy() {
        JsonObject properties = new JsonObject();
        String loadBalancePolicyStr = "RandomPolicy";
        properties.putString(virtualhost.getPersistencePolicyFieldName(), loadBalancePolicyStr);

        virtualhost.setProperties(properties);
        virtualhost.addClient(endpoint, true);

        assertThat(virtualhost.getChoice(false).toString()).isEqualTo(endpoint);
    }

    @Test
    public void clearProperties() {
        virtualhost.clearProperties();

        assertThat(virtualhost.getProperties()).isEqualTo(new JsonObject());
    }
}
