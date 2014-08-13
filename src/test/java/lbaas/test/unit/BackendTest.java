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

        HttpClient httpClient = backendTested.connect();
        assertThat(httpClient).isNotNull();

        testComplete();
    }

    @Test
    public void connectSuccessful() {
        Backend backendTested = new Backend(null, vertx);

        backendTested.connect();

        assertThat(backendTested.isClosed()).isFalse();

        testComplete();
    }

    @Test
    public void closeSuccessful() {
        Backend backendTested = new Backend(null, vertx);

        backendTested.connect();
        backendTested.close();

        assertThat(backendTested.isClosed()).isTrue();

        testComplete();
    }

}
