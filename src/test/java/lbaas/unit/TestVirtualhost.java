package lbaas.unit;

import static org.assertj.core.api.Assertions.*;
import static lbaas.unit.VirtualHostAssert.*;

import lbaas.Virtualhost;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;

public class TestVirtualhost {

    String virtualhostName;
    Vertx vertx;
    Virtualhost virtualhost;
    String endpoint;

    @Before
    public void setUp(){
        virtualhostName = "virtualhost1";
        vertx = null;
        virtualhost = new Virtualhost(virtualhostName, vertx);
        endpoint = "0.0.0.0:00";
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
}
