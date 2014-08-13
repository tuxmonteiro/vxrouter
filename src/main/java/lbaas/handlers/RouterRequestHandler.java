/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.handlers;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

import java.util.Map;

import lbaas.Backend;
import lbaas.ICounter;
import lbaas.RequestData;
import lbaas.Server;
import lbaas.Virtualhost;
import lbaas.exceptions.BadRequestException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Container;

public class RouterRequestHandler implements Handler<HttpServerRequest> {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;
    private final Map<String, Virtualhost> virtualhosts;
    private final Server server;
    private final Container container;
    private final ICounter counter;
    private String headerHost = "";
    private String backendId = "";
    private String counterKey = null;

    @Override
    public void handle(final HttpServerRequest sRequest) {

        final Long keepAliveTimeOut = conf.getLong("keepAliveTimeOut", 2000L);
        final Long keepAliveMaxRequest = conf.getLong("maxKeepAliveRequests", 100L);
        final Integer backendRequestTimeOut = conf.getInteger("backendRequestTimeOut", 60000);
        final Integer backendConnectionTimeOut = conf.getInteger("backendConnectionTimeOut", 60000);
        final Boolean backendForceKeepAlive = conf.getBoolean("backendForceKeepAlive", true);
        final Integer backendMaxPoolSize = conf.getInteger("backendMaxPoolSize",1);
        final boolean enableChunked = conf.getBoolean("enableChunked", true);

        sRequest.response().setChunked(true);

        final Long requestTimeoutTimer = vertx.setTimer(backendRequestTimeOut, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                server.showErrorAndClose(sRequest, new java.util.concurrent.TimeoutException(), getCounterKey(headerHost, backendId));
            }
        });

        if (sRequest.headers().contains("Host")) {
            this.headerHost = sRequest.headers().get("Host").split(":")[0];
            if (!virtualhosts.containsKey(headerHost)) {
                log.error(String.format("Host: %s UNDEF", headerHost));
                server.showErrorAndClose(sRequest, new BadRequestException(), getCounterKey(headerHost, backendId));
                return;
            }
        } else {
            log.error("Host UNDEF");
            server.showErrorAndClose(sRequest, new BadRequestException(), getCounterKey(headerHost, backendId));
            return;
        }

        final Virtualhost virtualhost = virtualhosts.get(headerHost);

        if (!virtualhost.hasBackends()) {
            log.error(String.format("Host %s without backends", headerHost));
            server.showErrorAndClose(sRequest, new BadRequestException(), getCounterKey(headerHost, backendId));
            return;
        }

        final boolean connectionKeepalive = isHttpKeepAlive(sRequest.headers(), sRequest.version());

        final Backend backend = virtualhost.getChoice(new RequestData(sRequest))
                .setKeepAlive(connectionKeepalive||backendForceKeepAlive)
                .setKeepAliveTimeOut(keepAliveTimeOut)
                .setKeepAliveMaxRequest(keepAliveMaxRequest)
                .setConnectionTimeout(backendConnectionTimeOut)
                .setMaxPoolSize(backendMaxPoolSize);
        this.backendId = backend.toString();

        Long initialRequestTime = System.currentTimeMillis();
        final Handler<HttpClientResponse> handlerHttpClientResponse =
                new RouterResponseHandler(vertx, container , requestTimeoutTimer, sRequest,
                        connectionKeepalive, backendForceKeepAlive, backend, server, counter,
                        headerHost, initialRequestTime);

        String remoteIP = sRequest.remoteAddress().getAddress().getHostAddress();
        String remotePort = String.format("%d", sRequest.remoteAddress().getPort());

        final HttpClient httpClient;
        try {
            httpClient = backend.connect();
            backend.addConnection(remoteIP, remotePort);

        } catch (RuntimeException e) {
            log.error(e.getMessage());
            server.showErrorAndClose(sRequest, e, getCounterKey(headerHost, backendId));
            return;
        }

        if (httpClient!=null && headerHost!=null) {
            counter.incrActiveSessions(getCounterKey(headerHost, backendId));
        }

        final HttpClientRequest cRequest =
                httpClient.request(sRequest.method(), sRequest.uri(), handlerHttpClientResponse)
                    .setChunked(enableChunked);

        updateHeadersXFF(sRequest.headers(), remoteIP);

        cRequest.headers().set(sRequest.headers());
        if (backendForceKeepAlive) {
            cRequest.headers().set("Connection", "keep-alive");
        }

        if (enableChunked) {
            // Pump sRequest => cRequest
            Pump.createPump(sRequest, cRequest).start();
        } else {
            sRequest.bodyHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                    cRequest.headers().set("Content-Length", String.format("%d", buffer.length()));
                    cRequest.write(buffer);
                }
            });
        }

        cRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, backend.toString() );
                server.showErrorAndClose(sRequest, event, getCounterKey(headerHost, backendId));
                try {
                    backend.close();
                } catch (RuntimeException e) {
                    // Ignore double backend close
                    return;
                }
            }
         });

        sRequest.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                cRequest.end();
            }
         });
    }

    public RouterRequestHandler(
            final Vertx vertx,
            final Container container,
            final Map<String, Virtualhost> virtualhosts,
            final Server server,
            final ICounter counter) {
        this.vertx = vertx;
        this.container = container;
        this.conf = container.config();
        this.log = container.logger();
        this.virtualhosts = virtualhosts;
        this.server = server;
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
        String xff;
        headers.set("X-Real-IP", remote);

        if (headers.contains("X-Forwarded-For")) {
            xff = String.format("%s, %s", headers.get("X-Forwarded-For"),remote);
            headers.remove("X-Forwarded-For");
        } else {
            xff = remote;
        }
        headers.set("X-Forwarded-For", xff);

        if (headers.contains("Forwarded-For")) {
            xff = String.format("%s, %s" , headers.get("Forwarded-For"), remote);
            headers.remove("Forwarded-For");
        } else {
            xff = remote;
        }
        headers.set("Forwarded-For", xff);

        if (!headers.contains("X-Forwarded-Host")) {
            headers.set("X-Forwarded-Host", this.headerHost);
        }

        if (!headers.contains("X-Forwarded-Proto")) {
            headers.set("X-Forwarded-Proto", "http");
        }
    }

    public boolean isHttpKeepAlive(MultiMap headers, HttpVersion httpVersion) {
        return headers.contains("Connection") ?
                !"close".equalsIgnoreCase(headers.get("Connection")) :
                httpVersion.equals(HttpVersion.HTTP_1_1);
    }

}
