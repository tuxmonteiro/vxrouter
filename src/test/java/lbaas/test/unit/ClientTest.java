/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.vertx.testtools.VertxAssert.testComplete;
import lbaas.Client;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.http.HttpClient;
import org.vertx.testtools.TestVerticle;

public class ClientTest extends TestVerticle {

    @Before
    public void setUp() {
    }

    @Test
    public void equalsObject() {
        Client client1 = new Client("127.0.0.1:0", vertx);
        Client client2 = new Client("127.0.0.1:0", vertx);

        assertThat(client1).isEqualTo(client2);

        testComplete();
    }

    @Test
    public void notEqualsObject() {
        Client client1 = new Client("127.0.0.1:0", vertx);
        Client client2 = new Client("127.0.0.2:0", vertx);

        assertThat(client1).isNotEqualTo(client2);

        testComplete();
    }

    @Test
    public void isKeepAliveLimitMaxRequest() {
        Client clientTested = new Client("127.0.0.1:0", vertx);

        clientTested.setKeepAliveMaxRequest(1L);
        boolean isKeepAliveLimitExceeded = clientTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isTrue();

        testComplete();
    }

    @Test
    public void isNotKeepAliveLimitMaxRequest() {
        Client clientTested = new Client("127.0.0.1:0", vertx);

        clientTested.setKeepAliveMaxRequest(2L);
        boolean isKeepAliveLimitExceeded = clientTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isFalse();

        testComplete();
    }

    @Test
    public void isKeepAliveLimitTimeOut() {
        Client clientTested = new Client("127.0.0.1:0", vertx);

        clientTested.setKeepAliveTimeOut(-1L);
        boolean isKeepAliveLimitExceeded = clientTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isTrue();

        testComplete();
    }

    @Test
    public void isNotKeepAliveLimitTimeOut() {
        Client clientTested = new Client("127.0.0.1:0", vertx);

        clientTested.setKeepAliveTimeOut(86400000L);
        boolean isKeepAliveLimitExceeded = clientTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isFalse();

        testComplete();
    }

    @Test
    public void connectReturnNotNull() {
        Client clientTested = new Client(null, vertx);

        HttpClient httpClient = clientTested.connect();
        assertThat(httpClient).isNotNull();

        testComplete();
    }


    @Test
    public void connectSuccessful() {
        Client clientTested = new Client(null, vertx);

        clientTested.connect();

        assertThat(clientTested.isClosed()).isFalse();

        testComplete();
    }

    @Test
    public void closeSuccessful() {
        Client clientTested = new Client(null, vertx);

        clientTested.connect();
        clientTested.close();

        assertThat(clientTested.isClosed()).isTrue();

        testComplete();
    }

}
