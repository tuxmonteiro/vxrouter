package lbaas.unit;

import static org.assertj.core.api.Assertions.*;

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

        boolean notExist = virtualhost.addClient(endpoint, endPointOk);

        assertThat(notExist).isTrue();
        assertThat(virtualhost.getClients(endPointOk)).hasSize(1);
    }

    @Test
    public void insertNewBadClientInSet() {
        boolean endPointOk = false;

        boolean notExist = virtualhost.addClient(endpoint, endPointOk);

        assertThat(notExist).isTrue();
        assertThat(virtualhost.getClients(endPointOk)).hasSize(1);
    }

    @Test
    public void insertDuplicatedClientInSet() {
        boolean endPointOk = true;

        virtualhost.addClient(endpoint, endPointOk);
        boolean notExist = virtualhost.addClient(endpoint, endPointOk);

        assertThat(notExist).isFalse();
        assertThat(virtualhost.getClients(endPointOk)).hasSize(1);
    }

    @Test
    public void insertDuplicatedBadClientInSet() {
        boolean endPointOk = false;

        virtualhost.addClient(endpoint, endPointOk);
        boolean notExist = virtualhost.addClient(endpoint, endPointOk);

        assertThat(notExist).isFalse();
        assertThat(virtualhost.getClients(endPointOk)).hasSize(1);
    }
}
