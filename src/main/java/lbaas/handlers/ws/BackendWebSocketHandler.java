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

import java.util.Queue;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.logging.Logger;

public class BackendWebSocketHandler implements Handler<WebSocket> {

    private final Vertx vertx;
    private final Logger log;
    private final String backendId;
    private final ServerWebSocket serverWebSocket;
    private final Queue<String> messages;

    private WebSocket websocket;
    private Long initialRequestTime = null;

    public BackendWebSocketHandler(
            final Vertx vertx,
            final Logger log,
            final String backendId,
            final ServerWebSocket serverWebSocket,
            final Queue<String> messages) {
        this.vertx = vertx;
        this.backendId = backendId;
        this.serverWebSocket = serverWebSocket;
        this.log = log;
        this.messages = messages;
    }

    @Override
    public void handle(final WebSocket websocket) {

        this.websocket = websocket;
        websocket.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.error(String.format("backend: %s, message: %s", backendId, event.getMessage()));
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, backendId );
                websocket.close();
            }
        });

        writeWebSocket(websocket, messages);

        websocket.dataHandler(new Handler<Buffer>() {

            @Override
            public void handle(Buffer buffer) {
                writeServerWebSocket(buffer);
            }

        });
    }

    public void writeWebSocket(final WebSocket ws, final Queue<String> messages) {
        while (!messages.isEmpty()) {
            ws.writeTextFrame(messages.poll());
        }
    }

    public void writeWebSocket(final WebSocket ws, String message) {
        ws.writeTextFrame(message);
    }

    public void writeServerWebSocket(Buffer buffer) {
        if (serverWebSocket!=null) {
            serverWebSocket.write(buffer);
        }
    }
    public Long getInitialRequestTime() {
        return initialRequestTime;
    }

    public BackendWebSocketHandler setInitialRequestTime(Long initialRequestTime) {
        this.initialRequestTime = initialRequestTime;
        return this;
    }

    public void checkMessages() {
        if (websocket!=null) {
            writeWebSocket(websocket, messages);
        }
    }

}
