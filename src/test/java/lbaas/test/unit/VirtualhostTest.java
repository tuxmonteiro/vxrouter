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

import static org.assertj.core.api.Assertions.*;
import static lbaas.core.Constants.*;
import static lbaas.test.unit.assertj.custom.VirtualHostAssert.*;
import lbaas.core.RequestData;
import lbaas.core.Virtualhost;
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
    String backend;
    RequestData requestData;

    @Before
    public void setUp(){
        virtualhostName = "virtualhost1";
        vertx = null;
        requestData = new RequestData();
        virtualhost = new Virtualhost(virtualhostName, vertx);
        backend = "0.0.0.0:0";
    }

    @Test
    public void insertNewBackendInSet() {
        boolean backendOk = true;

        boolean notFail = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(1, backendOk);
    }

    @Test
    public void insertNewBadBackendInSet() {
        boolean backendOk = false;

        boolean notExist = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notExist).hasSize(1, backendOk);
    }

    @Test
    public void insertDuplicatedBackendInSet() {
        boolean backendOk = true;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(1, backendOk);

    }

    @Test
    public void insertDuplicatedBadBackendInSet() {
        boolean backendOk = false;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(1, backendOk);

    }

    @Test
    public void removeExistingBackendInSet() {
        boolean backendOk = true;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(0, backendOk);
    }

    @Test
    public void removeExistingBadBackendInSet() {
        boolean backendOk = false;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(0, backendOk);
    }

    @Test
    public void removeAbsentBackendInSet() {
        boolean backendOk = true;

        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(0, backendOk);

    }

    @Test
    public void removeAbsentBadBackendInSet() {
        boolean backendOk = false;

        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(0, backendOk);

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
    public void getBackendWithLoadBalancePolicy() {
        virtualhost.putString(loadBalancePolicyFieldName, DefaultLoadBalancePolicy.class.getSimpleName());

        virtualhost.addBackend(backend, true);

        assertThat(virtualhost.getChoice(requestData).toString()).isEqualTo(backend);
    }

    @Test
    public void getBackendWithPersistencePolicy() {
        virtualhost.putString(persistencePolicyFieldName, DefaultLoadBalancePolicy.class.getSimpleName());

        virtualhost.addBackend(backend, true);

        assertThat(virtualhost.getChoice(requestData, false).toString()).isEqualTo(backend);
    }

}
