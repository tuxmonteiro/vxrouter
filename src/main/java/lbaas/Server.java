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
package lbaas;

import static lbaas.Constants.CONF_PORT;
import static lbaas.Constants.CONF_ENABLE_ACCESSLOG;
import io.netty.handler.codec.http.HttpResponseStatus;
import lbaas.exceptions.BadRequestException;
import lbaas.logger.impl.NcsaLogExtendedFormatter;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

public class Server {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;
    private final ICounter counter;
    private final boolean enableAccessLog;
    private final String HttpHeadersHost = HttpHeaders.HOST.toString();


    public Server(final Vertx vertx, final Container container, final ICounter counter) {
        this.vertx = vertx;
        this.conf = container.config();
        this.log = container.logger();
        this.counter = counter;
        this.enableAccessLog = this.conf.getBoolean(CONF_ENABLE_ACCESSLOG, false);
    }

    public void start(
            final Object caller,
            final Handler<HttpServerRequest> handlerHttpServerRequest,
            final Integer defaultPort) {

        final Integer port = conf.getInteger(CONF_PORT,defaultPort);

        vertx.createHttpServer().requestHandler(handlerHttpServerRequest)
            .setTCPKeepAlive(conf.getBoolean("serverTCPKeepAlive",true))
            .listen(port, new Handler<AsyncResult<HttpServer>>() {
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
    }

    public void setStatusCode(final HttpServerResponse resp, Integer code, String messageCode) {
        resp.setStatusCode(code);
        String message = messageCode != null ? messageCode : HttpResponseStatus.valueOf(code).reasonPhrase();
        resp.setStatusMessage(message);
    }

    public void setHeaders(final HttpServerResponse resp, final MultiMap headers) {
        resp.headers().set(headers);
    }

    public void showErrorAndClose(final HttpServerRequest req, final Throwable event, String key) {

        int statusCode;
        if (event instanceof java.util.concurrent.TimeoutException) {
            statusCode = 504;
        } else if (event instanceof BadRequestException) {
            statusCode = 400;
        } else {
            statusCode = 502;
        }

        setStatusCode(req.response(), statusCode, null);
        returnStatus(req, key);
        String message = String.format("FAIL with HttpStatus %d (virtualhost %s): %s",
                statusCode,
                req.headers().contains("Host")? req.headers().get(HttpHeadersHost): "UNDEF",
                HttpResponseStatus.valueOf(statusCode).reasonPhrase());

        if (statusCode>499) {
            log.error(message);
        } else {
            log.warn(message);
        }

        close(req);
    }

    public void close(final HttpServerRequest req) {
        try {
            req.response().close();
        } catch (RuntimeException e) {
            // Ignore already closed
            return;
        }
    }

    public void returnStatus(final HttpServerRequest req, String id) {
        returnStatus(req, "", id);
    }

    public void returnStatus(final HttpServerRequest req, String message, String id) {

        Integer code = req.response().getStatusCode();

        logRequest(enableAccessLog, req);
        sendRequestCount(id, code);

        if (!"".equals(message)) {
            JsonObject json = new JsonObject(message);
            message = json.encodePrettily();
        }

        responseEnd(req.response(), message);
    }

    public void responseEnd(final HttpServerResponse resp, String message) {
        Integer code = resp.getStatusCode();

        try {
            if (!"".equals(message)) {

                resp.end(message);
            } else {
                resp.end();
            }
        } catch (RuntimeException e) {
            // java.lang.IllegalStateException: Response has already been written ?
            log.error(String.format("FAIL: statusCode %d, Error > %s", code, e.getMessage()));
            return;
        }
    }

    public void logRequest(boolean enable, final HttpServerRequest req) {

        if (enableAccessLog) {
            Integer code = req.response().getStatusCode();
            String message = "";
            int codeFamily = code.intValue()/100;
                String httpLogMessage = new NcsaLogExtendedFormatter()
                                            .setRequestData(req, message)
                                            .getFormatedLog();
            switch (codeFamily) {
                case 5: // SERVER_ERROR
                    log.error(httpLogMessage);
                    break;
                case 0: // OTHER,
                case 1: // INFORMATIONAL
                case 2: // SUCCESSFUL
                case 3: // REDIRECTION
                case 4: // CLIENT_ERROR
                default:
                    log.info(httpLogMessage);
                    break;
            }
        }
    }

    public void sendRequestCount(String id, int code) {
        if (counter!=null && id!=null) {
            counter.httpCode(id, code);
        }
    }

}
