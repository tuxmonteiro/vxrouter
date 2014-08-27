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
package lbaas.logger.impl;

import java.util.Calendar;

import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;

import lbaas.logger.HttpLogFormatter;

public class NcsaLogExtendedFormatter implements HttpLogFormatter {

    private HttpServerRequest req;

    @Override
    public HttpLogFormatter setRequestData(final Object request, String message) {
        this.req = (HttpServerRequest) request;
        return this;
    }

    @Override
    public String getFormatedLog() {
        if (req!=null) {

            // format: virtualhost remotehost rfc931 authuser [date] "method request_uri version" status bytes

            String virtualhost = req.headers().contains(HttpHeaders.HOST) ?
                                    req.headers().get(HttpHeaders.HOST) : "-";
            String remotehost = req.remoteAddress().getHostString();
            String rfc931 = "-";
            String authuser = "-";
            String date = req.headers().contains(HttpHeaders.DATE) ?
                            req.headers().get(HttpHeaders.DATE) :
                            Calendar.getInstance().getTime().toString();
            String method = req.method();
            HttpVersion httpVersion = req.version();
            String version = "";
            switch (httpVersion) {
                case HTTP_1_0:
                    version = "HTTP/1.0";
                    break;
                case HTTP_1_1:
                    version = "HTTP/1.1";
                    break;
                default:
                    version = httpVersion.toString();
                    break;
            }
            String request_uri = req.path();
            int status = req.response().getStatusCode();
            int bytes = 0;
            try {
                bytes = req.headers().contains(HttpHeaders.CONTENT_LENGTH) ?
                            Integer.parseInt(req.headers().get(HttpHeaders.CONTENT_LENGTH)) : 0;
            } catch (NumberFormatException e) {
                // ignore
            }
            return String.format("%s %s %s %s [%s] \"%s %s %s\" %d %d",
                    virtualhost,
                    remotehost,
                    rfc931,
                    authuser,
                    date,
                    method,
                    request_uri,
                    version,
                    status,
                    bytes);
        } else {
            return null;
        }
    }

}
