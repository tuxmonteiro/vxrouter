/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.vertx.testtools.VertxAssert.testComplete;
import lbaas.Backend;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.http.HttpClient;
import org.vertx.testtools.TestVerticle;

public class BackendTest extends TestVerticle {

    @Before
    public void setUp() {
    }

    @Test
    public void equalsObject() {
        Backend backend1 = new Backend("127.0.0.1:0", vertx);
        Backend backend2 = new Backend("127.0.0.1:0", vertx);

        assertThat(backend1).isEqualTo(backend2);

        testComplete();
    }

    @Test
    public void notEqualsObject() {
        Backend backend1 = new Backend("127.0.0.1:0", vertx);
        Backend backend2 = new Backend("127.0.0.2:0", vertx);

        assertThat(backend1).isNotEqualTo(backend2);

        testComplete();
    }

    @Test
    public void isKeepAliveLimitMaxRequest() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveMaxRequest(1L);
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isTrue();

        testComplete();
    }

    @Test
    public void isNotKeepAliveLimitMaxRequest() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveMaxRequest(2L);
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isFalse();

        testComplete();
    }

    @Test
    public void isKeepAliveLimitTimeOut() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveTimeOut(-1L);
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isTrue();

        testComplete();
    }

    @Test
    public void isNotKeepAliveLimitTimeOut() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveTimeOut(86400000L);
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isFalse();

        testComplete();
    }

    @Test
    public void connectReturnNotNull() {
        Backend backendTested = new Backend(null, vertx);

        HttpClient httpClient = backendTested.connect("127.0.0.1", "0");
        assertThat(httpClient).isNotNull();

        testComplete();
    }

    @Test
    public void connectSuccessful() {
        Backend backendTested = new Backend(null, vertx);

        backendTested.connect("127.0.0.1", "0");

        assertThat(backendTested.isClosed()).isFalse();

        testComplete();
    }

    @Test
    public void closeSuccessful() {
        Backend backendTested = new Backend(null, vertx);

        backendTested.connect("127.0.0.1", "0");
        backendTested.close();

        assertThat(backendTested.isClosed()).isTrue();

        testComplete();
    }

    @Test
    public void zeroActiveConnections() {
        Backend backendTested = new Backend(null, vertx);

        assertThat(backendTested.getActiveConnections()).isEqualTo(0);

        testComplete();
    }

    @Test
    public void multiplesActiveConnections() {
        Backend backendTested = new Backend(null, vertx);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(String.format("%s", counter), "0");
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1000);

        testComplete();
    }

    @Test
    public void multiplesRequestsButOneActiveConnection() {
        Backend backendTested = new Backend(null, vertx);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect("127.0.0.1", "0");
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1);

        testComplete();
    }

    @Test
    public void zeroActiveConnectionsBecauseMaxRequestsExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveMaxRequest(1000L);
        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(String.format("%s", counter), "0");
            boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();
            if (isKeepAliveLimitExceeded) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(0);

        testComplete();
    }

    @Test
    public void multiplesActiveConnectionsBecauseMaxRequestsNotExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveMaxRequest(1001L);
        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(String.format("%s", counter), "0");
            boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();
            if (isKeepAliveLimitExceeded) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isNotEqualTo(0);

        testComplete();
    }

    @Test
    public void zeroActiveConnectionsBecauseTimeoutExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);
        backendTested.setKeepAliveTimeOut(-1L);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(String.format("%s", counter), "0");
            if (backendTested.isKeepAliveLimit()) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(0);

        testComplete();
    }

    @Test
    public void multiplesActiveConnectionsBecauseTimeoutNotExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);
        backendTested.setKeepAliveTimeOut(86400000L); // one day

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(String.format("%s", counter), "0");
            if (backendTested.isKeepAliveLimit()) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isNotEqualTo(0);

        testComplete();
    }
}
