/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.handlers;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;
import lbaas.Client;
import lbaas.Server;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.streams.Pump;

public class RouterResponseHandler implements Handler<HttpClientResponse> {

    private final Vertx vertx;
    private final Long requestTimeoutTimer;
    private final HttpServerRequest sRequest;
    private final boolean connectionKeepalive;
    private final boolean clientForceKeepAlive;
    private final Client client;
    private final Server server;

    @Override
    public void handle(HttpClientResponse cResponse) {
 
        vertx.cancelTimer(requestTimeoutTimer);

        // Pump cResponse => sResponse
        sRequest.response().headers().set(cResponse.headers());
        if (!connectionKeepalive) {
            sRequest.response().headers().set("Connection", "close");
        }

        Pump.createPump(cResponse, sRequest.response()).start();

        cResponse.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                server.returnStatus(sRequest, 200, null);

                if (connectionKeepalive) {
                    if (client.isKeepAliveLimit()) {
                        client.close();
                        server.close(sRequest);
                    }
                } else {
                    if (!clientForceKeepAlive) {
                        client.close();
                    }
                    server.close(sRequest);
                }
            }
        });

        cResponse.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, client.toString() );
                server.showErrorAndClose(sRequest, event);
                client.close();
            }
        });

    }

    public RouterResponseHandler(
            final Vertx vertx, 
            final Long requestTimeoutTimer, 
            final HttpServerRequest sRequest,
            final boolean connectionKeepalive,
            final boolean clientForceKeepAlive,
            final Client client,
            final Server server) {
        this.vertx = vertx;
        this.requestTimeoutTimer = requestTimeoutTimer;
        this.sRequest = sRequest;
        this.connectionKeepalive = connectionKeepalive;
        this.clientForceKeepAlive = clientForceKeepAlive;
        this.client = client;
        this.server = server;
    }
}
