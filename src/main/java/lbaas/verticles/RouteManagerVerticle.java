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
package lbaas.verticles;

import static lbaas.core.Constants.QUEUE_ROUTE_ADD;
import static lbaas.core.Constants.QUEUE_ROUTE_DEL;
import static lbaas.core.Constants.QUEUE_ROUTE_VERSION;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lbaas.core.Backend;
import lbaas.core.IEventObserver;
import lbaas.core.QueueMap;
import lbaas.core.Server;
import lbaas.core.Virtualhost;
import lbaas.exceptions.RouterException;
import lbaas.handlers.ServerResponse;
import lbaas.metrics.CounterWithStatsd;
import lbaas.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class RouteManagerVerticle extends Verticle implements IEventObserver {
    private static String routeManagerId = "route_manager";

    private final Map<String, Virtualhost> virtualhosts = new HashMap<>();
    private Server server;

    private Long version = 0L;

    private enum Action {
        ADD,
        DEL,
        VERSION
    }

    @Override
    public void start() {
        final Logger log = container.logger();
        final JsonObject conf = container.config();
        final ICounter counter = new CounterWithStatsd(conf, vertx, log);
        server = new Server(vertx, container, counter);

        startHttpServer(conf);
        final QueueMap queueMap = new QueueMap(this, virtualhosts);
        queueMap.registerQueueAdd();
        queueMap.registerQueueDel();
        queueMap.registerQueueVersion();

        log.info(String.format("Instance %s started", this.toString()));
    }

    @Override
    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public void postAddEvent(String message) {
        return;
    };

    @Override
    public void postDelEvent(String message) {
        return;
    };

    private long getVersion() {
        return this.version;
    }

    private String getStatusMessageOk() {
        return "{\"status_message\": \"OK\"}";
    }

    private String getStatusMessageFail() {
        return "{\"status_message\": \"Bad Request\"}";
    }

    private Handler<HttpServerRequest> routeHandlerAction(final Action action) {
        final Logger log = this.getContainer().logger();
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                final String virtualHost = req.params() != null && req.params().contains("id") ? req.params().get("id") : "";
                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        try {
                            JsonObject json;
                            if (!"".equals(body.toString())) {
                                json = new JsonObject(body.toString());
                                String jsonVirtualHost;
                                if (!"".equals(virtualHost)) {
                                    jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                                    if (!jsonVirtualHost.equalsIgnoreCase(virtualHost)) {
                                        throw new RouterException("Virtualhost: inconsistent reference");
                                    }
                                }
                            } else {
                                json = new JsonObject();
                            }
                            serverResponse.setStatusCode(200, null);
                            serverResponse.end(getStatusMessageOk(), routeManagerId);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("routeHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            serverResponse.setStatusCode(400, null);
                            serverResponse.end(getStatusMessageFail(), routeManagerId);
                        }
                    }
                });
            }
        };
    }

    // TODO: REFACTOR THIS, PLEASE
    private void startHttpServer(final JsonObject serverConf) throws RuntimeException {
        final EventBus eb = this.getVertx().eventBus();
        final Logger log = this.getContainer().logger();
        final Server server = this.server;

        RouteMatcher routeMatcher = new RouteMatcher();

        // FULL Infos/Changes
        routeMatcher.post("/route", routeHandlerAction(Action.ADD));
        routeMatcher.post("/route/:vh", routeHandlerAction(Action.ADD));

        routeMatcher.get("/route", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);
                serverResponse.setStatusCode(200, null);
                serverResponse.end(getRoutes().encodePrettily(), routeManagerId);
                log.info("GET /route");
            }
        });

        routeMatcher.delete("/route", routeHandlerAction(Action.DEL));

        // Version
        routeMatcher.post("/version", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);

                        try {
                            JsonObject json = new JsonObject(body.toString());
                            if (json.containsField("version")) {
                                sendAction(String.format("%d", json.getLong("version")), Action.VERSION);
                            }
                            serverResponse.setStatusCode(200, null);
                            serverResponse.end(getStatusMessageOk(),  routeManagerId);
                        } catch (RuntimeException e) {
                            serverResponse.setStatusCode(400, null);
                            serverResponse.end(getStatusMessageFail(), routeManagerId);
                        }
                    }
                });
            }
        });
        routeMatcher.get("/version",new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                JsonObject versionJson = new JsonObject(String.format("{\"version\":%d}", getVersion()));

                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);
                serverResponse.setStatusCode(200, null);
                serverResponse.end(versionJson.encodePrettily(), routeManagerId);
                log.info(String.format("GET /version: %d", getVersion()));
            }
        });

        // VirtualHost
        routeMatcher.post("/virtualhost", virtualhostHandlerAction(Action.ADD));

        routeMatcher.delete("/virtualhost", virtualhostHandlerAction(Action.DEL)); // ALL
        routeMatcher.delete("/virtualhost/:id", virtualhostHandlerAction(Action.DEL)); // Only ID

        routeMatcher.get("/virtualhost", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);
                serverResponse.setStatusCode(200, null);
                serverResponse.end(getVirtualHosts(""), routeManagerId);
                log.info("GET /virtualhost");
            }
        });

        routeMatcher.get("/virtualhost/:id", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                String virtualhost = req.params() != null && req.params().contains("id") ? req.params().get("id"): "";

                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);
                serverResponse.setStatusCode(200, null);
                serverResponse.end(getVirtualHosts(virtualhost), routeManagerId);
                log.info(String.format("GET /virtualhost/%s", virtualhost));
            }
        });

        // Backend (field "Virtualhost name" mandatory) - Only POST and DELETE
        routeMatcher.post("/backend", backendHandlerAction(eb, log, Action.ADD));
        routeMatcher.delete("/backend/:id", backendHandlerAction(eb, log, Action.DEL)); // Only with ID

        // Others methods/uris/etc
        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);
                serverResponse.setStatusCode(400, null);
                serverResponse.end(getStatusMessageFail(), routeManagerId);
                log.warn(String.format("%s %s not supported", req.method(), req.uri()));
            }
        });

        server.setDefaultPort(9090).setHttpServerRequestHandler(routeMatcher).start(this);
    }

    private Handler<HttpServerRequest> backendHandlerAction(final EventBus eb, final Logger log, final Action action) {
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);

                String[] backendWithPort = null;
                try {
                    backendWithPort = req.params() != null && req.params().contains("id") ?
                            java.net.URLDecoder.decode(req.params().get("id"), "UTF-8").split(":") : null;
                } catch (UnsupportedEncodingException e) {}

                final String backend = backendWithPort != null ? backendWithPort[0]:"";
                final String port = backendWithPort != null ? backendWithPort[1]:"";
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        try {
                            final JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                            if (action==Action.DEL) {
                                JsonArray backends = json.containsField("backends") ? json.getArray("backends"): null;
                                if (backends!=null && !backends.toList().isEmpty() && !backends.get(0).equals(new JsonObject(String.format("{\"host\":\"%s\",\"port\":%s}", backend, port)))) {
                                    throw new RouterException("Backend not found");
                                }
                            }
                            if ("".equals(jsonVirtualHost)) {
                                throw new RouterException("Virtualhost name null");
                            }
                            serverResponse.setStatusCode(200, null);
                            serverResponse.end(getStatusMessageOk(), routeManagerId);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("backendHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            serverResponse.setStatusCode(400, null);
                            serverResponse.end(getStatusMessageFail(), routeManagerId);
                        }
                    }
                });
            }
        };
    }

    private String getVirtualHosts(String virtualhost) {
        if (!"".equals(virtualhost)) {
            JsonArray routes = getRoutes().getArray("routes");
            Iterator<Object> it = routes.iterator();
            while (it.hasNext()) {
                JsonObject route = (JsonObject) it.next();
                if (route.getString("name").equalsIgnoreCase(virtualhost)) {
                    return route.encodePrettily();
                }
            }
            return new JsonObject("{}").encodePrettily();
        }
        return getRoutes().encodePrettily();
    }

    private Handler<HttpServerRequest> virtualhostHandlerAction(final Action action) {
        final Logger log = this.getContainer().logger();

        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req.response(), log, null, false);

                String virtualhostRequest = "";
                try {
                    virtualhostRequest = req.params() != null && req.params().contains("id") ?
                            java.net.URLDecoder.decode(req.params().get("id"), "UTF-8") : "";
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                final String virtualhost = virtualhostRequest;
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        try {
                            final JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                            if ("".equals(jsonVirtualHost)) {
                                throw new RouterException("Virtualhost name null");
                            }
                            if (action==Action.DEL && !jsonVirtualHost.equals(virtualhost) && "".equals(virtualhost)) {
                                throw new RouterException("Virtualhost: inconsistent reference");
                            }
                            serverResponse.setStatusCode(200, null);
                            serverResponse.end(getStatusMessageOk(), routeManagerId);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("virtualHostHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            serverResponse.setStatusCode(400, null);
                            serverResponse.end(getStatusMessageFail(), routeManagerId);
                        }
                    }
                });
            }
        };
    }

    public void setRoute(final JsonObject json, final Action action, final String uri) throws RuntimeException {
        JsonArray jsonRoutes = null;
        if (json.containsField("routes")) {
            jsonRoutes = json.getArray("routes");
        } else {
            jsonRoutes = new JsonArray();
            jsonRoutes.addObject(json);
        }

        Iterator<Object> it = jsonRoutes.iterator();
        while (it.hasNext()) {
            String vhost;
            JsonObject properties;
            String host;
            Integer port;
            boolean status;
            JsonArray backends = null;
            JsonObject jsonTemp = (JsonObject) it.next();

            if (jsonTemp.containsField("name")) {
                vhost = jsonTemp.getString("name");
            } else {
                throw new RouterException("virtualhost undef");
            }
            if (jsonTemp.containsField("properties")) {
                try {
                    properties = jsonTemp.getObject("properties");
                } catch (DecodeException e) {
                    properties = new JsonObject();
                }
            } else {
                properties = new JsonObject();
            }

            if (jsonTemp.containsField("backends") && jsonTemp.getArray("backends").size()>0) {
                backends = jsonTemp.getArray("backends");
                Iterator<Object> backendsIterator = backends.iterator();
                while (backendsIterator.hasNext()) {
                    JsonObject backendJson = (JsonObject) backendsIterator.next();
                    host = backendJson.containsField("host") ? backendJson.getString("host"):"";
                    port = backendJson.containsField("port") ? backendJson.getInteger("port"):null;
                    String portStr = String.format("%d", port);
                    status = backendJson.containsField("status") ? backendJson.getBoolean("status"):true;
                    String statusStr = status ? "1" : "0";
                    if ("".equals(host) || port==null) {
                        throw new RouterException("Backend host or port undef");
                    }
                    String message = QueueMap.buildMessage(vhost,
                                                           host,
                                                           portStr,
                                                           statusStr,
                                                           uri,
                                                           properties.toString());
                    sendAction(message, action);
                }
            } else {
                String message = QueueMap.buildMessage(vhost, "", "", "", uri, properties.toString());
                sendAction(message, action);
            }

        }
    }

    private void sendAction(String message, Action action) {
        final EventBus eb = this.getVertx().eventBus();
        final Logger log = this.getContainer().logger();

        switch (action) {
            case ADD:
                eb.publish(QUEUE_ROUTE_ADD, message);
                log.debug(String.format("Sending %s to %s",message, QUEUE_ROUTE_ADD));
                break;
            case DEL:
                eb.publish(QUEUE_ROUTE_DEL, message);
                log.debug(String.format("Sending %s to %s",message, QUEUE_ROUTE_DEL));
                break;
            case VERSION:
                eb.publish(QUEUE_ROUTE_VERSION, message);
                log.debug(String.format("Sending %s to %s",message, QUEUE_ROUTE_VERSION));
                break;
            default:
                throw new RouterException("Action not supported");
        }
    }

     private JsonObject getRoutes() {
        JsonObject routes = new JsonObject();
        routes.putNumber("version", getVersion());
        JsonArray vhosts = new JsonArray();

        for (String vhost : virtualhosts.keySet()) {
            JsonObject vhostObj = new JsonObject();
            vhostObj.putString("name", vhost);
            JsonArray backends = new JsonArray();
            Virtualhost virtualhost = virtualhosts.get(vhost);
            if (virtualhost==null) {
                continue;
            }
            vhostObj.putObject("properties", virtualhost.copy());
            for (Backend value : virtualhost.getBackends(true)) {
                if (value!=null) {
                    JsonObject backendObj = new JsonObject();
                    backendObj.putString("host", value.toString().split(":")[0]);
                    backendObj.putNumber("port", Integer.parseInt(value.toString().split(":")[1]));
                    backends.add(backendObj);
                }
            }
            vhostObj.putArray("backends", backends);
            JsonArray badBackends = new JsonArray();
            if (!virtualhost.getBackends(false).isEmpty()) {
                for (Backend value : virtualhost.getBackends(false)) {
                    if (value!=null) {
                        JsonObject backendObj = new JsonObject();
                        String[] hostWithPort = value.toString().split(":");
                        backendObj.putString("host", hostWithPort[0]);
                        backendObj.putNumber("port", Integer.parseInt(hostWithPort[1]));
                        badBackends.add(backendObj);
                    }
                }
            }
            vhostObj.putArray("badBackends", badBackends);

            vhosts.add(vhostObj);
        }
        routes.putArray("routes", vhosts);
        return routes;
    }

}
