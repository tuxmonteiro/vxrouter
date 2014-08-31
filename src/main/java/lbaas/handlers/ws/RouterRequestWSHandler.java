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

import java.util.Map;

import lbaas.core.Backend;
import lbaas.core.RequestData;
import lbaas.core.Virtualhost;
import lbaas.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Container;

public class RouterRequestWSHandler implements Handler<ServerWebSocket> {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;
    private final Map<String, Virtualhost> virtualhosts;
    private final Container container;
    private final ICounter counter;
    private String headerHost = "";
    private String backendId = "";
    private String counterKey = null;
    private final String httpHeaderHost = HttpHeaders.HOST.toString();
    private final String httpHeaderConnection = HttpHeaders.CONNECTION.toString();


    @Override
    public void handle(final ServerWebSocket sRequest) {

        log.debug(String.format("Received request for host %s '%s'",
                sRequest.headers().get(httpHeaderHost), sRequest.uri()));

        final Integer backendRequestTimeOut = conf.getInteger("backendRequestTimeOut", 60000);
        final Integer backendConnectionTimeOut = conf.getInteger("backendConnectionTimeOut", 60000);
        final Integer backendMaxPoolSize = conf.getInteger("backendMaxPoolSize",10);
        final boolean enableAccessLog = conf.getBoolean("enableAccessLog", false);
//        final ServerResponse<ServerWebSocket> sResponse = new ServerResponse<>(sRequest,
//                log, counter, enableAccessLog);

        final Long requestTimeoutTimer = vertx.setTimer(backendRequestTimeOut, new Handler<Long>() {
            @Override
            public void handle(Long event) {
//                sResponse.showErrorAndClose(new java.util.concurrent.TimeoutException(), getCounterKey(headerHost, backendId));
            }
        });

        if (sRequest.headers().contains(httpHeaderHost)) {
            this.headerHost = sRequest.headers().get(httpHeaderHost).split(":")[0];
            if (!virtualhosts.containsKey(headerHost)) {
                vertx.cancelTimer(requestTimeoutTimer);
                log.warn(String.format("Host: %s UNDEF", headerHost));
//                sResponse.showErrorAndClose(new BadRequestException(), null);
                return;
            }
        } else {
            vertx.cancelTimer(requestTimeoutTimer);
            log.warn("Host UNDEF");
//            sResponse.showErrorAndClose(new BadRequestException(), null);
            return;
        }

        final Virtualhost virtualhost = virtualhosts.get(headerHost);

        if (!virtualhost.hasBackends()) {
            vertx.cancelTimer(requestTimeoutTimer);
            log.warn(String.format("Host %s without backends", headerHost));
//            sResponse.showErrorAndClose(new BadRequestException(), null);
            return;
        }

        final Backend backend = virtualhost.getChoice(new RequestData(sRequest))
                .setKeepAlive(true)
                .setKeepAliveTimeOut(Long.MAX_VALUE)
                .setKeepAliveMaxRequest(Long.MAX_VALUE)
                .setConnectionTimeout(backendConnectionTimeOut)
                .setMaxPoolSize(backendMaxPoolSize);

        this.backendId = backend.toString();

        Long initialRequestTime = System.currentTimeMillis();
        final Handler<WebSocket> handlerHttpClientResponse =
                new RouterResponseWSHandler(vertx,
                                          container.logger(),
                                          requestTimeoutTimer,
                                          sRequest,
                                          backend,
                                          counter)
                        .setHeaderHost(headerHost)
                        .setInitialRequestTime(initialRequestTime);

        String remoteIP = sRequest.remoteAddress().getAddress().getHostAddress();
        String remotePort = String.format("%d", sRequest.remoteAddress().getPort());

        final HttpClient httpClient = backend.connect(remoteIP, remotePort);

        if (httpClient!=null && headerHost!=null && backend.getSessionController().isNewConnection(remoteIP, remotePort)) {
            counter.sendActiveSessions(getCounterKey(headerHost, backendId),1L);
        }

        httpClient.connectWebsocket(sRequest.uri(), handlerHttpClientResponse);

        WebSocket webSocket = ((RouterResponseWSHandler) handlerHttpClientResponse).getWebsocket();

        updateHeadersXFF(sRequest.headers(), remoteIP);

        Pump.createPump(sRequest, webSocket).start();

        webSocket.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                vertx.cancelTimer(requestTimeoutTimer);
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, backend.toString() );
//                sResponse.showErrorAndClose(event, getCounterKey(headerHost, backendId));
                try {
                    backend.close();
                } catch (RuntimeException e) {
                    // Ignore double backend close
                    return;
                }
            }
         });

    }

    public RouterRequestWSHandler(
            final Vertx vertx,
            final Container container,
            final Map<String, Virtualhost> virtualhosts,
            final ICounter counter) {
        this.vertx = vertx;
        this.container = container;
        this.conf = container.config();
        this.log = container.logger();
        this.virtualhosts = virtualhosts;
        this.counter = counter;
    }

    public String getHeaderHost() {
        return headerHost;
    }

    public void setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
    }

    public String getBackendId() {
        return backendId;
    }

    public void setBackendId(String backendId) {
        this.backendId = backendId;
    }

    public String getCounterKey(String aVirtualhost, String aBackend) {
        if (counterKey==null || "".equals(counterKey)) {
            String strDefault = "UNDEF";
            String result = String.format("%s.%s",
                    counter.cleanupString(aVirtualhost, strDefault),
                    counter.cleanupString(aBackend, strDefault));
            if (!"".equals(aVirtualhost) && !"".equals(aBackend)) {
                counterKey = result;
            }
            return result;
        } else {
            return counterKey;
        }
    }

    private void updateHeadersXFF(final MultiMap headers, String remote) {

        final String httpHeaderXRealIp         = "X-Real-IP";
        final String httpHeaderXForwardedFor   = "X-Forwarded-For";
        final String httpHeaderforwardedFor    = "Forwarded-For";
        final String httpHeaderXForwardedHost  = "X-Forwarded-Host";
        final String httpHeaderXForwardedProto = "X-Forwarded-Proto";

        if (!headers.contains(httpHeaderXRealIp)) {
            headers.set(httpHeaderXRealIp, remote);
        }

        String xff;
        if (headers.contains(httpHeaderXForwardedFor)) {
            xff = String.format("%s, %s", headers.get(httpHeaderXForwardedFor),remote);
            headers.remove(httpHeaderXForwardedFor);
        } else {
            xff = remote;
        }
        headers.set(httpHeaderXForwardedFor, xff);

        if (headers.contains(httpHeaderforwardedFor)) {
            xff = String.format("%s, %s" , headers.get(httpHeaderforwardedFor), remote);
            headers.remove(httpHeaderforwardedFor);
        } else {
            xff = remote;
        }
        headers.set(httpHeaderforwardedFor, xff);

        if (!headers.contains(httpHeaderXForwardedHost)) {
            headers.set(httpHeaderXForwardedHost, this.headerHost);
        }

        if (!headers.contains(httpHeaderXForwardedProto)) {
            headers.set(httpHeaderXForwardedProto, "http");
        }
    }

    public boolean isHttpKeepAlive(MultiMap headers, HttpVersion httpVersion) {
        return headers.contains(httpHeaderConnection) ?
                !"close".equalsIgnoreCase(headers.get(httpHeaderConnection)) :
                httpVersion.equals(HttpVersion.HTTP_1_1);
    }

}
