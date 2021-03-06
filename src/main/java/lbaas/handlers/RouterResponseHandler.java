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
package lbaas.handlers;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;
import lbaas.Backend;
import lbaas.ICounter;
import lbaas.Server;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Container;

public class RouterResponseHandler implements Handler<HttpClientResponse> {

    private final Vertx vertx;
    private final Long requestTimeoutTimer;
    private final HttpServerRequest sRequest;
    private final boolean connectionKeepalive;
    private final boolean backendForceKeepAlive;
    private final Backend backend;
    private final Server server;
    private final ICounter counter;
    private final Logger log;
    private final String headerHost;
    private Long initialRequestTime;

    @Override
    public void handle(final HttpClientResponse cResponse) {
        log.debug(String.format("Received response from backend %d %s", cResponse.statusCode(), cResponse.statusMessage()));

        vertx.cancelTimer(requestTimeoutTimer);

        // Define statusCode and Headers
        final int statusCode = cResponse.statusCode();
        server.setStatusCode(sRequest.response(), statusCode, cResponse.statusMessage());
        server.setHeaders(sRequest.response(), cResponse.headers());
        if (!connectionKeepalive) {
            sRequest.response().headers().set("Connection", "close");
        }

        // Pump cResponse => sResponse
        Pump.createPump(cResponse, sRequest.response()).start();

        cResponse.endHandler(new VoidHandler() {
            @Override
            public void handle() {

                if (headerHost!=null) {
                    if (initialRequestTime!=null) {
                        counter.requestTime(getKey(), initialRequestTime);
                    }
                }

                server.setStatusCode(sRequest.response(), statusCode, "");
                server.returnStatus(sRequest, getKey());

                if (connectionKeepalive) {
                    if (backend.isKeepAliveLimit()) {
                        backend.close();
                        server.close(sRequest);
                    }
                } else {
                    if (!backendForceKeepAlive) {
                        backend.close();
                    }
                    server.close(sRequest);
                }
            }
        });

        cResponse.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.error(String.format("host+backend: %s, message: %s", getKey(), event.getMessage()));
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, backend.toString() );
                server.showErrorAndClose(sRequest, event, getKey());
                backend.close();
            }
        });

    }

    private String getKey() {
        return String.format("%s.%s",
                headerHost!=null?headerHost.replaceAll("[^\\w]", "_"):"UNDEF",
                backend!=null?backend.toString().replaceAll("[^\\w]", "_"):"UNDEF");
    }

    public RouterResponseHandler(
            final Vertx vertx,
            final Container container,
            final Long requestTimeoutTimer,
            final HttpServerRequest sRequest,
            final boolean connectionKeepalive,
            final boolean backendForceKeepAlive,
            final Backend backend,
            final Server server,
            final ICounter counter,
            final String headerHost,
            final Long initialRequestTime) {
        this.vertx = vertx;
        this.requestTimeoutTimer = requestTimeoutTimer;
        this.sRequest = sRequest;
        this.connectionKeepalive = connectionKeepalive;
        this.backendForceKeepAlive = backendForceKeepAlive;
        this.backend = backend;
        this.server = server;
        this.log = container.logger();
        this.headerHost = headerHost;
        this.initialRequestTime = initialRequestTime;
        this.counter = counter;
    }

}
