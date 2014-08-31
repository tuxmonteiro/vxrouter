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

import io.netty.handler.codec.http.HttpResponseStatus;
import lbaas.exceptions.BadRequestException;
import lbaas.logger.impl.NcsaLogExtendedFormatter;
import lbaas.metrics.ICounter;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class ServerResponse {

    private final HttpServerResponse resp;
    private final Logger log;
    private final ICounter counter;
    private final boolean enableAccessLog;
    private final String httpHeaderHost = HttpHeaders.HOST.toString();


    public ServerResponse(final HttpServerResponse resp,
                          final Logger log,
                          final ICounter counter,
                          boolean enableAccessLog) {
        this.log = log;
        this.counter = counter;
        this.enableAccessLog = enableAccessLog;
        this.resp = resp;
    }

    public void setStatusCode(Integer code, String messageCode) {
        resp.setStatusCode(code);
        String message = messageCode != null ? messageCode : HttpResponseStatus.valueOf(code).reasonPhrase();
        resp.setStatusMessage(message);
    }

    public void setHeaders(final MultiMap headers) {
        resp.headers().set(headers);
    }

    public void showErrorAndClose(final Throwable event, String key) {

        int statusCode;
        if (event instanceof java.util.concurrent.TimeoutException) {
            statusCode = 504;
        } else if (event instanceof BadRequestException) {
            statusCode = 400;
        } else {
            statusCode = 502;
        }

        setStatusCode(statusCode, null);
        end(key);
        String message = String.format("FAIL with HttpStatus %d (virtualhost %s): %s",
                statusCode,
                resp.headers().contains(httpHeaderHost)? resp.headers().get(httpHeaderHost): "UNDEF",
                HttpResponseStatus.valueOf(statusCode).reasonPhrase());

        if (statusCode>499) {
            log.error(message);
        } else {
            log.warn(message);
        }

        closeResponse();
    }

    public void closeResponse() {
        try {
            resp.close();
        } catch (RuntimeException ignoreAlreadyClose) {
            return;
        }
    }

    private void realEnd(String message) {
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

    public void end(String id) {
        end("", id);
    }

    public void end(String message, String id) {

        Integer code = resp.getStatusCode();

        logRequest(enableAccessLog);
        sendRequestCount(id, code);

        if (!"".equals(message)) {
            JsonObject json = new JsonObject(message);
            message = json.encodePrettily();
        }

        realEnd(message);
    }

    public void logRequest(boolean enable) {

        if (enableAccessLog) {
            Integer code = resp.getStatusCode();
            String message = "";
            int codeFamily = code.intValue()/100;
            // TODO: Dependency Injection
            String httpLogMessage = new NcsaLogExtendedFormatter()
                                        .setRequestData(resp.headers().contains(httpHeaderHost) ?
                                                resp.headers().get(httpHeaderHost):"-", message)
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
