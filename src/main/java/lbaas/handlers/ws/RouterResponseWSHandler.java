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
package lbaas.handlers.ws;

import static lbaas.core.Constants.QUEUE_HEALTHCHECK_FAIL;
import lbaas.core.Backend;
import lbaas.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;

public class RouterResponseWSHandler implements Handler<WebSocket> {

    private final Vertx vertx;
    private final Long requestTimeoutTimer;
    private final ServerWebSocket sRequest;
    private final Backend backend;
    private final ICounter counter;
    private final Logger log;

    private WebSocket websocket;

    private String headerHost = "UNDEF";
    private Long initialRequestTime = null;
    private boolean connectionKeepalive = true;
    private boolean backendForceKeepAlive = true;

    @Override
    public void handle(final WebSocket websocket) {

        this.websocket = websocket;
        vertx.cancelTimer(requestTimeoutTimer);

        // Pump cResponse => sResponse
        Pump.createPump(websocket, sRequest).start();

        websocket.endHandler(new VoidHandler() {
            @Override
            public void handle() {

                if (headerHost!=null) {
                    if (initialRequestTime!=null) {
                        counter.requestTime(getKey(), initialRequestTime);
                    }
                }

                if (connectionKeepalive) {
                    if (backend.isKeepAliveLimit()) {
                        backend.close();
                    }
                } else {
                    if (!backendForceKeepAlive) {
                        backend.close();
                    }
                }
            }
        });

        websocket.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.error(String.format("host+backend: %s, message: %s", getKey(), event.getMessage()));
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, backend.toString() );
                backend.close();
            }
        });

    }

    public String getHeaderHost() {
        return headerHost;
    }

    public RouterResponseWSHandler setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
        return this;
    }

    public Long getInitialRequestTime() {
        return initialRequestTime;
    }

    public RouterResponseWSHandler setInitialRequestTime(Long initialRequestTime) {
        this.initialRequestTime = initialRequestTime;
        return this;
    }

    public WebSocket getWebsocket() {
        return websocket;
    }

    private String getKey() {
        return String.format("%s.%s",
                headerHost!=null?headerHost.replaceAll("[^\\w]", "_"):"UNDEF",
                backend!=null?backend.toString().replaceAll("[^\\w]", "_"):"UNDEF");
    }

    public RouterResponseWSHandler(
            final Vertx vertx,
            final Logger log,
            final Long requestTimeoutTimer,
            final ServerWebSocket sRequest,
            final Backend backend,
            final ICounter counter) {
        this.vertx = vertx;
        this.requestTimeoutTimer = requestTimeoutTimer;
        this.sRequest = sRequest;
        this.backend = backend;
        this.log = log;
        this.counter = counter;
    }

}
