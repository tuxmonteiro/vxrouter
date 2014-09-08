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

import java.util.Map;
import java.util.Map.Entry;
import lbaas.core.Backend;
import lbaas.core.RequestData;
import lbaas.core.Virtualhost;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.WebSocketVersion;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class FrontendWebSocketHandler implements Handler<ServerWebSocket> {

    private final Vertx vertx;
//    private final JsonObject conf;
    private final Logger log;
    private final Map<String, Virtualhost> virtualhosts;
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    public FrontendWebSocketHandler(
            final Vertx vertx,
            final Container container,
            final Map<String, Virtualhost> virtualhosts) {
        this.vertx = vertx;
//        this.conf = container.config();
        this.log = container.logger();
        this.virtualhosts = virtualhosts;
    }

    @Override
    public void handle(final ServerWebSocket serverWebSocket) {

        String hostnameWithPort = "";
        for (Entry<String, String> e: serverWebSocket.headers().entries()) {
            if (e.getKey().equalsIgnoreCase(httpHeaderHost)) {
                hostnameWithPort = e.getValue();
            }
        }
        log.info(String.format("Received request for host %s '%s'",
                hostnameWithPort, serverWebSocket.uri()));

        if ("".equals(hostnameWithPort)) {
            log.warn("Host UNDEF");
            serverWebSocket.close();
            return;
        }

        String hostname = hostnameWithPort.split(":")[0];

        if (!virtualhosts.containsKey(hostname)) {
            log.warn(String.format("Host: %s UNDEF", hostname));
            serverWebSocket.close();
            return;
        }

        final Virtualhost virtualhost = virtualhosts.get(hostname);

        if (!virtualhost.hasBackends()) {
            log.warn(String.format("Host %s without backends", hostname));
            serverWebSocket.close();
            return;
        }

        final Backend backend = virtualhost.getChoice(new RequestData(serverWebSocket))
                .setKeepAlive(true)
                .setKeepAliveTimeOut(Long.MAX_VALUE)
                .setKeepAliveMaxRequest(Long.MAX_VALUE)
                .setConnectionTimeout(10000)
                .setMaxPoolSize(10);

        String backendId = backend.toString();
        log.info(backend);

        String remoteIP = serverWebSocket.remoteAddress().getAddress().getHostAddress();
        String remotePort = String.format("%d", serverWebSocket.remoteAddress().getPort());

        final HttpClient httpClient = backend.connect(remoteIP, remotePort);
        final BackendWebSocketHandler backendWebSocketHandler =
                new BackendWebSocketHandler(vertx, log, backendId, serverWebSocket);

        httpClient.connectWebsocket(serverWebSocket.uri(),
                WebSocketVersion.RFC6455, serverWebSocket.headers(), backendWebSocketHandler);

        serverWebSocket.dataHandler(new Handler<Buffer>() {

            @Override
            public void handle(Buffer buffer) {
                backendWebSocketHandler.forwardToBackend(buffer);
            }

        });

        serverWebSocket.closeHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                log.debug("Frontend WebSocket was closed");
                backendWebSocketHandler.closeWS();
            }
        });

    }
}
