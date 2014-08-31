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
package lbaas.core;

import static lbaas.core.Constants.CONF_PORT;
import lbaas.metrics.ICounter;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class Server {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;
    private final HttpServer httpServer;

    private Integer port = 9000;

    public Server(final Vertx vertx, final Container container, final ICounter counter) {
        this.vertx = vertx;
        this.conf = container.config();
        this.log = container.logger();
        this.httpServer = vertx.createHttpServer().setTCPKeepAlive(this.conf.getBoolean("serverTCPKeepAlive",true));
    }

    public Server start(final Object caller) {

        this.port = conf.getInteger(CONF_PORT, port);

        try {
        httpServer.listen(port, new Handler<AsyncResult<HttpServer>>() {
                @Override
                public void handle(AsyncResult<HttpServer> asyncResult) {
                    if (asyncResult.succeeded()) {
                        log.info(String.format("[%s] Server listen: %d/tcp", caller.toString(), port));
                        EventBus eb = vertx.eventBus();
                        eb.publish("init.server", String.format("{ \"id\": \"%s\", \"status\": \"started\" }", caller.toString()));
                    } else {
                        log.fatal(String.format("[%s] Could not start server port: %d/tcp", caller.toString(), port));
                    }
                }
            });
        } catch (RuntimeException e) {
            log.error(e.getMessage());
            log.debug(e.getStackTrace());
        }
        return this;
    }

    public Server setHttpServerRequestHandler(final Handler<HttpServerRequest> httpServerRequestHandler) {
        httpServer.requestHandler(httpServerRequestHandler);
        return this;
    }

    public Server setWebsocketServerRequestHandler(final Handler<ServerWebSocket> websocketServerRequestHandler) {
        httpServer.websocketHandler(websocketServerRequestHandler);
        return this;
    }

    public Server setDefaultPort(Integer defaultPort) {
        this.port = defaultPort;
        return this;
    }

}
