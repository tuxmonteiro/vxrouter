/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import static lbaas.Constants.CONF_PORT;
import io.netty.handler.codec.http.HttpResponseStatus;
import lbaas.exceptions.BadRequestException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class Server {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;

    public Server(final Vertx vertx, final Container container) {
        this.vertx = vertx;
        this.conf = container.config();
        this.log = container.logger();
    }

    public void start(final Object caller, final Handler<HttpServerRequest> handlerHttpServerRequest, final Integer defaultPort) {
        Integer port = conf.getInteger(CONF_PORT,defaultPort);
        vertx.createHttpServer().requestHandler(handlerHttpServerRequest)
            .setTCPKeepAlive(conf.getBoolean("serverTCPKeepAlive",true))
            .listen(port);
        log.info(String.format("[%s] Server listen: %d/tcp", caller.toString(), port));
    }

    public void showErrorAndClose(final HttpServerResponse serverResponse, final Throwable event) {

        if (event instanceof java.util.concurrent.TimeoutException) {
            serverResponse.setStatusCode(504);
            serverResponse.setStatusMessage("Gateway Time-Out");
        } else if (event instanceof BadRequestException) {
            serverResponse.setStatusCode(400);
            serverResponse.setStatusMessage("Bad Request");
        } else {
            serverResponse.setStatusCode(502);
            serverResponse.setStatusMessage("Bad Gateway");
        }

        try {
            serverResponse.end();
        } catch (java.lang.IllegalStateException e) {
            // Response has already been written ?
            return;
        }

        close(serverResponse);
    }

    public void close(final HttpServerResponse serverResponse) {
        try {
            serverResponse.close();
        } catch (RuntimeException e) {
            // Ignore already closed
            return;
        }
    }


    public void returnStatus(final HttpServerRequest req, Integer code) {
        returnStatus(req, code, "");
    }

    public void returnStatus(final HttpServerRequest req, Integer code, String message) {
        req.response().setStatusCode(code);
        req.response().setStatusMessage(HttpResponseStatus.valueOf(code).reasonPhrase());
        req.response().headers().set("Content-Type", "application/json");
        String messageReturn = message;
        if ("".equals(message)) {
            JsonObject json = new JsonObject(String.format("{ \"status_message\":\"%s\"}", req.response().getStatusMessage()));
            messageReturn = json.encodePrettily();
        }
        req.response().end(messageReturn);
    }

}
