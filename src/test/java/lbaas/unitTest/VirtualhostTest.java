/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.unitTest;

import static org.assertj.core.api.Assertions.*;
import static lbaas.Constants.*;
import static lbaas.unitTest.assertj.custom.VirtualHostAssert.*;
import lbaas.RequestData;
import lbaas.Virtualhost;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.loadbalance.impl.DefaultLoadBalancePolicy;
import lbaas.loadbalance.impl.RandomPolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;

public class VirtualhostTest {

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
    public void loadBalancePolicyClassFound() {
        virtualhost.putString(loadBalancePolicyFieldName, RandomPolicy.class.getSimpleName());

        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy();

        assertThat(loadBalance.isDefault()).isFalse();
    }

    @Test
    public void loadBalancePolicyClassNotFound() {
        String loadBalancePolicyStr = "ClassNotExist";
        virtualhost.putString(loadBalancePolicyFieldName, loadBalancePolicyStr);

        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy();

        assertThat(loadBalance.isDefault()).isTrue();
    }

    @Test
    public void getClientWithLoadBalancePolicy() {
        virtualhost.putString(loadBalancePolicyFieldName, DefaultLoadBalancePolicy.class.getSimpleName());

        virtualhost.addClient(endpoint, true);

        assertThat(virtualhost.getChoice(requestData).toString()).isEqualTo(endpoint);
    }

    @Test
    public void getClientWithPersistencePolicy() {
        virtualhost.putString(persistencePolicyFieldName, DefaultLoadBalancePolicy.class.getSimpleName());

        virtualhost.addClient(endpoint, true);

        assertThat(virtualhost.getChoice(requestData, false).toString()).isEqualTo(endpoint);
    }

}
