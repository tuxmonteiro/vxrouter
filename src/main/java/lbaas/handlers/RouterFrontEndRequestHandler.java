/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.handlers;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

import java.util.Map;
import java.util.Set;

import lbaas.Client;
import lbaas.Server;
import lbaas.exceptions.BadRequestException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Container;

public class RouterFrontEndRequestHandler implements Handler<HttpServerRequest> {

    private final Vertx vertx;
    private final JsonObject conf;
    private final Logger log;
    private final Map<String, Set<Client>> vhosts;
    private final Server server;

    @Override
    public void handle(final HttpServerRequest sRequest) {

        final Long keepAliveTimeOut = conf.getLong("keepAliveTimeOut", 2000L);
        final Long keepAliveMaxRequest = conf.getLong("maxKeepAliveRequests", 100L);
        final Integer clientRequestTimeOut = conf.getInteger("clientRequestTimeOut", 60000);
        final Integer clientConnectionTimeOut = conf.getInteger("clientConnectionTimeOut", 60000);
        final Boolean clientForceKeepAlive = conf.getBoolean("clientForceKeepAlive", true);
        final Integer clientMaxPoolSize = conf.getInteger("clientMaxPoolSize",1);

        sRequest.response().setChunked(true);

        final Long requestTimeoutTimer = vertx.setTimer(clientRequestTimeOut, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                server.showErrorAndClose(sRequest.response(), new java.util.concurrent.TimeoutException());
            }
        });

        String headerHost;
        if (sRequest.headers().contains("Host")) {
            headerHost = sRequest.headers().get("Host").split(":")[0];
            if (!vhosts.containsKey(headerHost)) {
                log.error(String.format("Host: %s UNDEF", headerHost));
                server.showErrorAndClose(sRequest.response(), new BadRequestException());
                return;
            }
        } else {
            log.error("Host UNDEF");
            server.showErrorAndClose(sRequest.response(), new BadRequestException());
            return;
        }

        final Set<Client> clients = vhosts.get(headerHost);
        if (clients==null ? true : clients.isEmpty()) {
            log.error(String.format("Host %s without endpoints", headerHost));
            server.showErrorAndClose(sRequest.response(), new BadRequestException());
            return;
        }

        final boolean connectionKeepalive = sRequest.headers().contains("Connection") ?
                !"close".equalsIgnoreCase(sRequest.headers().get("Connection")) : 
                sRequest.version().equals(HttpVersion.HTTP_1_1);

        final Client client = ((Client)clients.toArray()[getChoice(clients.size())])
                .setKeepAlive(connectionKeepalive||clientForceKeepAlive)
                .setKeepAliveTimeOut(keepAliveTimeOut)
                .setKeepAliveMaxRequest(keepAliveMaxRequest)
                .setConnectionTimeout(clientConnectionTimeOut)
                .setMaxPoolSize(clientMaxPoolSize);

        final Handler<HttpClientResponse> handlerHttpClientResponse = 
                new RouterBackEndResponseHandler(vertx, requestTimeoutTimer, sRequest, 
                        connectionKeepalive, clientForceKeepAlive, client, server);
        final HttpClient httpClient = client.connect();
        final HttpClientRequest cRequest =
                httpClient.request(sRequest.method(), sRequest.uri(),handlerHttpClientResponse)
                    .setChunked(true);

        changeHeader(sRequest, headerHost);

        cRequest.headers().set(sRequest.headers());
        if (clientForceKeepAlive) {
            cRequest.headers().set("Connection", "keep-alive");
        }

        // Pump sRequest => cRequest
        Pump.createPump(sRequest, cRequest).start();

        cRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, client.toString() );

                server.showErrorAndClose(sRequest.response(), event);
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

    public RouterFrontEndRequestHandler(
            final Vertx vertx, 
            final Container container, 
            final Map<String, Set<Client>> vhosts,
            final Server server) {
        this.vertx = vertx;
        this.conf = container.config();
        this.log = container.logger();
        this.vhosts = vhosts;
        this.server = server;
    }

    private void changeHeader(final HttpServerRequest sRequest, final String vhost) {
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
            sRequest.headers().set("X-Forwarded-Host", vhost);
        }

        if (!sRequest.headers().contains("X-Forwarded-Proto")) {
            sRequest.headers().set("X-Forwarded-Proto", "http");
        }
    }

    private int getChoice(int size) {
        return (int) (Math.random() * (size - Float.MIN_VALUE));
    }

}
