/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.verticles;

import static lbaas.Constants.QUEUE_ROUTE_ADD;
import static lbaas.Constants.QUEUE_ROUTE_DEL;
import static lbaas.Constants.QUEUE_ROUTE_VERSION;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lbaas.Client;
import lbaas.CounterWithStatsd;
import lbaas.ICounter;
import lbaas.IEventObserver;
import lbaas.QueueMap;
import lbaas.Server;
import lbaas.Virtualhost;
import lbaas.exceptions.RouterException;

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

    private Handler<HttpServerRequest> routeHandlerAction(final Action action) {
        final Logger log = this.getContainer().logger();
        final Server server = this.server;
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                final String virtualHost = req.params() != null && req.params().contains("id") ? req.params().get("id") : "";
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        try {
                            JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost;
                            if (!"".equals(virtualHost)) {
                                jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                                if (!jsonVirtualHost.equalsIgnoreCase(virtualHost)) {
                                    throw new RouterException("Virtualhost: inconsistent reference");
                                }
                            }
                            server.returnStatus(req,200, "", routeManagerId);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("routeHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            server.returnStatus(req,400, "", routeManagerId);
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
                server.returnStatus(req, 200, getRoutes().encodePrettily(), routeManagerId);
                log.info("GET /route");
            }
        });

        // Version
        routeMatcher.post("/version", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        try {
                            JsonObject json = new JsonObject(body.toString());
                            if (json.containsField("version")) {
                                sendAction(String.format("%d", json.getLong("version")), Action.VERSION);
                            }
                            server.returnStatus(req, 200, "", routeManagerId);
                        } catch (RuntimeException e) {
                            server.returnStatus(req, 400, "", routeManagerId);
                        }
                    }
                });
            }
        });
        routeMatcher.get("/version",new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                JsonObject versionJson = new JsonObject(String.format("{\"version\":%d}", getVersion()));
                server.returnStatus(req, 200, versionJson.encodePrettily(), routeManagerId);
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
                server.returnStatus(req, 200, getVirtualHosts(""), routeManagerId);
                log.info("GET /virtualhost");
            }
        });

        routeMatcher.get("/virtualhost/:id", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                String virtualhost = req.params() != null && req.params().contains("id") ? req.params().get("id"): "";
                server.returnStatus(req, 200, getVirtualHosts(virtualhost), routeManagerId);
                log.info(String.format("GET /virtualhost/%s", virtualhost));
            }
        });

        // Real (field "Virtualhost name" mandatory) - Only POST and DELETE
        routeMatcher.post("/real", realHandlerAction(eb, log, Action.ADD));
        routeMatcher.delete("/real/:id", realHandlerAction(eb, log, Action.DEL)); // Only with ID

        // Others methods/uris/etc
        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                server.returnStatus(req, 400, "", routeManagerId);
                log.warn(String.format("%s %s not supported", req.method(), req.uri()));
            }
        });

        server.start(this, routeMatcher, 9090);
    }

    private Handler<HttpServerRequest> realHandlerAction(final EventBus eb, final Logger log, final Action action) {
        final Server server = this.server;
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                String[] realWithPort = null;
                try {
                    realWithPort = req.params() != null && req.params().contains("id") ?
                            java.net.URLDecoder.decode(req.params().get("id"), "UTF-8").split(":") : null;
                } catch (UnsupportedEncodingException e) {}

                final String real = realWithPort != null ? realWithPort[0]:"";
                final String port = realWithPort != null ? realWithPort[1]:"";
                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        try {
                            final JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                            if (action==Action.DEL) {
                                JsonArray reals = json.containsField("endpoints") ? json.getArray("endpoints"): null;
                                if (reals!=null && !reals.toList().isEmpty() && !reals.get(0).equals(new JsonObject(String.format("{\"host\":\"%s\",\"port\":%s}", real, port)))) {
                                    throw new RouterException("Real not found");
                                }
                            }
                            if ("".equals(jsonVirtualHost)) {
                                throw new RouterException("Virtualhost name null");
                            }
                            server.returnStatus(req, 200, "", routeManagerId);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("realHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            server.returnStatus(req, 400, "", routeManagerId);
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
        final Server server = this.server;

        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
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
                            server.returnStatus(req, 200, "", routeManagerId);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("virtualHostHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            server.returnStatus(req, 400, "", routeManagerId);
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
            JsonArray endpoints = null;
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

            if (jsonTemp.containsField("endpoints")) {
                endpoints = jsonTemp.getArray("endpoints");
                Iterator<Object> endpointsIterator = endpoints.iterator();
                while (endpointsIterator.hasNext()) {
                    JsonObject endpointJson = (JsonObject) endpointsIterator.next();
                    host = endpointJson.containsField("host") ? endpointJson.getString("host"):"";
                    port = endpointJson.containsField("port") ? endpointJson.getInteger("port"):null;
                    String portStr = String.format("%d", port);
                    status = endpointJson.containsField("status") ? endpointJson.getBoolean("status"):true;
                    String statusStr = status ? "1" : "0";
                    if ("".equals(host) || port==null) {
                        throw new RouterException("Endpoint host or port undef");
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
            JsonArray endpoints = new JsonArray();
            Virtualhost virtualhost = virtualhosts.get(vhost);
            if (virtualhost==null) {
                continue;
            }
            JsonObject properties = virtualhost.getProperties();
            vhostObj.putObject("properties", properties);
            for (Client value : virtualhost.getClients(true)) {
                if (value!=null) {
                    JsonObject endpointObj = new JsonObject();
                    endpointObj.putString("host", value.toString().split(":")[0]);
                    endpointObj.putNumber("port", Integer.parseInt(value.toString().split(":")[1]));
                    endpoints.add(endpointObj);
                }
            }
            vhostObj.putArray("endpoints", endpoints);
            JsonArray badEndpoints = new JsonArray();
            if (!virtualhost.getClients(false).isEmpty()) {
                for (Client value : virtualhost.getClients(false)) {
                    if (value!=null) {
                        JsonObject endpointObj = new JsonObject();
                        String[] hostWithPort = value.toString().split(":");
                        endpointObj.putString("host", hostWithPort[0]);
                        endpointObj.putNumber("port", Integer.parseInt(hostWithPort[1]));
                        badEndpoints.add(endpointObj);
                    }
                }
            }
            vhostObj.putArray("badEndpoints", badEndpoints);

            vhosts.add(vhostObj);
        }
        routes.putArray("routes", vhosts);
        return routes;
    }

}
