/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.handlers;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

import java.util.Map;
import lbaas.Client;
import lbaas.ICounter;
import lbaas.Server;
import lbaas.Virtualhost;
import lbaas.exceptions.BadRequestException;

import org.vertx.java.core.Handler;
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
    private String headerHost = null;
    private String clientId = null;

    @Override
    public void handle(final HttpServerRequest sRequest) {

        final Long keepAliveTimeOut = conf.getLong("keepAliveTimeOut", 2000L);
        final Long keepAliveMaxRequest = conf.getLong("maxKeepAliveRequests", 100L);
        final Integer clientRequestTimeOut = conf.getInteger("clientRequestTimeOut", 60000);
        final Integer clientConnectionTimeOut = conf.getInteger("clientConnectionTimeOut", 60000);
        final Boolean clientForceKeepAlive = conf.getBoolean("clientForceKeepAlive", true);
        final Integer clientMaxPoolSize = conf.getInteger("clientMaxPoolSize",1);
        final boolean enableChunked = conf.getBoolean("enableChunked", true);

        sRequest.response().setChunked(true);

        final Long requestTimeoutTimer = vertx.setTimer(clientRequestTimeOut, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                server.showErrorAndClose(sRequest, new java.util.concurrent.TimeoutException(), getKey());
            }
        });

        if (sRequest.headers().contains("Host")) {
            this.headerHost = sRequest.headers().get("Host").split(":")[0];
            if (!virtualhosts.containsKey(headerHost)) {
                log.error(String.format("Host: %s UNDEF", headerHost));
                server.showErrorAndClose(sRequest, new BadRequestException(), getKey());
                return;
            }
        } else {
            log.error("Host UNDEF");
            server.showErrorAndClose(sRequest, new BadRequestException(), getKey());
            return;
        }

        final Virtualhost virtualhost = virtualhosts.get(headerHost);
        if (virtualhost.getClients(true).isEmpty()) {
            log.error(String.format("Host %s without endpoints", headerHost));
            server.showErrorAndClose(sRequest, new BadRequestException(), getKey());
            return;
        }

        final boolean connectionKeepalive = sRequest.headers().contains("Connection") ?
                !"close".equalsIgnoreCase(sRequest.headers().get("Connection")) :
                sRequest.version().equals(HttpVersion.HTTP_1_1);

        final Client client = ((Client) (virtualhost.getClients(true).toArray()[getChoice(virtualhost.getClients(true).size())]))
                .setKeepAlive(connectionKeepalive||clientForceKeepAlive)
                .setKeepAliveTimeOut(keepAliveTimeOut)
                .setKeepAliveMaxRequest(keepAliveMaxRequest)
                .setConnectionTimeout(clientConnectionTimeOut)
                .setMaxPoolSize(clientMaxPoolSize);

        this.clientId = client.toString();

        Long initialRequestTime = System.currentTimeMillis();
        final Handler<HttpClientResponse> handlerHttpClientResponse =
                new RouterResponseHandler(vertx, container , requestTimeoutTimer, sRequest,
                        connectionKeepalive, clientForceKeepAlive, client, server, counter,
                        headerHost, initialRequestTime);
        final HttpClient httpClient = client.connect();
        if (httpClient!=null && headerHost!=null) {
            counter.incrActiveSessions(getKey());
        }
        final HttpClientRequest cRequest =
                httpClient.request(sRequest.method(), sRequest.uri(),handlerHttpClientResponse)
                    .setChunked(enableChunked);

        changeHeader(sRequest);

        cRequest.headers().set(sRequest.headers());
        if (clientForceKeepAlive) {
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
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, client.toString() );
                server.showErrorAndClose(sRequest, event, getKey());
                try {
                    client.close();
                } catch (RuntimeException e) {
                    // Ignore double client close
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

    private void changeHeader(final HttpServerRequest sRequest) {
        String xff;
        String remote = sRequest.remoteAddress().getAddress().getHostAddress();
        sRequest.headers().set("X-Real-IP", remote);

        if (sRequest.headers().contains("X-Forwarded-For")) {
            xff = String.format("%s, %s", sRequest.headers().get("X-Forwarded-For"),remote);
            sRequest.headers().remove("X-Forwarded-For");
        } else {
            xff = remote;
        }
        sRequest.headers().set("X-Forwarded-For", xff);

        if (sRequest.headers().contains("Forwarded-For")) {
            xff = String.format("%s, %s" , sRequest.headers().get("Forwarded-For"), remote);
            sRequest.headers().remove("Forwarded-For");
        } else {
            xff = remote;
        }
        sRequest.headers().set("Forwarded-For", xff);

        if (!sRequest.headers().contains("X-Forwarded-Host")) {
            sRequest.headers().set("X-Forwarded-Host", this.headerHost);
        }

        if (!sRequest.headers().contains("X-Forwarded-Proto")) {
            sRequest.headers().set("X-Forwarded-Proto", "http");
        }
    }

    private int getChoice(int size) {
        return (int) (Math.random() * (size - Float.MIN_VALUE));
    }

    private String getKey() {
        return String.format("%s.%s",
                headerHost!=null?headerHost.replaceAll("[^\\w]", "_"):"UNDEF",
                clientId!=null?clientId.replaceAll("[^\\w]", "_"):"UNDEF");
    }

}
