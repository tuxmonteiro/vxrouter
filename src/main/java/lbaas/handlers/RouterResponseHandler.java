/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.handlers;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;
import lbaas.Client;
import lbaas.Server;
import lbaas.verticles.StatsDClient;
import lbaas.verticles.StatsDClient.TypeStatsdMessage;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Container;

public class RouterResponseHandler implements Handler<HttpClientResponse> {

    private final Vertx vertx;
    private final Long requestTimeoutTimer;
    private final HttpServerRequest sRequest;
    private final boolean connectionKeepalive;
    private final boolean clientForceKeepAlive;
    private final Client client;
    private final Server server;
    private final StatsDClient statsdClient;
    private final Logger log;
    private final JsonObject conf;
    private final String headerHost;
    private Long initialRequestTime;
    private final boolean enableStatsd;

    @Override
    public void handle(HttpClientResponse cResponse) {
 
        vertx.cancelTimer(requestTimeoutTimer);

        // Pump cResponse => sResponse
        sRequest.response().headers().set(cResponse.headers());
        if (!connectionKeepalive) {
            sRequest.response().headers().set("Connection", "close");
        }

        Pump.createPump(cResponse, sRequest.response()).start();

        cResponse.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                if (headerHost!=null && initialRequestTime!=null) {
                    sendRequestTime(headerHost.replace('.', '~'), initialRequestTime);
                }
                server.returnStatus(sRequest, 200, null);

                if (connectionKeepalive) {
                    if (client.isKeepAliveLimit()) {
                        client.close();
                        server.close(sRequest);
                    }
                } else {
                    if (!clientForceKeepAlive) {
                        client.close();
                    }
                    server.close(sRequest);
                }
            }
        });

        cResponse.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, client.toString() );
                server.showErrorAndClose(sRequest, event);
                client.close();
            }
        });

    }

    public RouterResponseHandler(
            final Vertx vertx,
            final Container container,
            final Long requestTimeoutTimer,
            final HttpServerRequest sRequest,
            final boolean connectionKeepalive,
            final boolean clientForceKeepAlive,
            final Client client,
            final Server server,
            final String headerHost,
            final Long initialRequestTime) {
        this.vertx = vertx;
        this.requestTimeoutTimer = requestTimeoutTimer;
        this.sRequest = sRequest;
        this.connectionKeepalive = connectionKeepalive;
        this.clientForceKeepAlive = clientForceKeepAlive;
        this.client = client;
        this.server = server;
        this.conf = container.config();
        this.log = container.logger();

        this.enableStatsd = conf.getBoolean("enableStatsd", false);
        if (enableStatsd) {
            this.headerHost = headerHost;
            this.initialRequestTime = initialRequestTime;
            String statsdHost = conf.getString("statsdHost","127.0.0.1");
            Integer statsdPort = conf.getInteger("statsdPort", 8125);
            this.statsdClient = new StatsDClient(statsdHost, statsdPort);
        } else {
            this.headerHost = null;
            this.initialRequestTime = null;
            this.statsdClient = null;
        }
    }

    private void sendRequestTime(final String virtualhost, final Long initialRequestTime) {
        Long requestTime = System.currentTimeMillis() - initialRequestTime;
        statsdClient.sendStatsd(TypeStatsdMessage.TIME,
                String.format("%s.requestTime:%d", virtualhost, requestTime), vertx, log);
    }

}
