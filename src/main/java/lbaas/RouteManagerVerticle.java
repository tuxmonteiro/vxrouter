package lbaas;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
import org.vertx.java.core.logging.Logger;

import static lbaas.Constants.QUEUE_ROUTE_ADD;
import static lbaas.Constants.QUEUE_ROUTE_DEL;
import static lbaas.Constants.CONF_PORT;

public class RouteManagerVerticle extends Verticle {

    private final HashMap<String, Set<String>> graphRoutes = new HashMap<>();
    private Character separator = ':';
    private Long version = 0L;

    private enum Action {
        ADD,
        DEL
    }

    public void start() {
        final EventBus eb = vertx.eventBus();
        final Logger log = container.logger();

        startHttpServer(eb, log, container.config());

        eb.registerHandler(QUEUE_ROUTE_ADD, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                final String[] route = message.body().split(separator.toString());
                if (route.length > 3) {
                    String endpoint = "";
                    if (!"".equals(route[1]) && !"".equals(route[2])) {
                        endpoint = String.format("%s:%s", route[1], route[2]);
                    }
                    if (graphRoutes.containsKey(route[0])) {
                        final Set<String> clients = graphRoutes.get(route[0]);
                        if (clients !=null) {
                            clients.add(endpoint);
                            log.info(String.format("[%s] Real %s (%s) added", route[3], endpoint, route[0]));
                        }
                    } else {
                        final Set<String> clients = new HashSet<String>();
                        graphRoutes.put(route[0], clients);
                        log.info(String.format("[%s] Virtualhost %s added", route[3], route[0]));

                        if (!"".equals(endpoint)) {
                            clients.add(endpoint);
                            log.info(String.format("[%s] Real %s (%s) added", route[3], endpoint, route[0]));
                        }
                    }
                    try {
                        version = Long.parseLong(route[3]);
                    } catch (NumberFormatException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        });

        eb.registerHandler(QUEUE_ROUTE_DEL, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                final String[] route = message.body().split(separator.toString());
                if (route.length > 3) {
                    String endpoint = "";
                    if (!"".equals(route[1]) && !"".equals(route[2])) {
                        endpoint = String.format("%s:%s", route[1], route[2]);
                    }
                    if (graphRoutes.containsKey(route[0])) {
                        Set<String> clients = graphRoutes.get(route[0]);
                        if (clients != null) {
                            clients.remove(endpoint);
                            log.info(String.format("[%s] Real %s (%s) removed", route[3], endpoint, route[0]));
                            if (clients.isEmpty()) {
                                clients = null;
                                graphRoutes.remove(route[0]);
                                log.info(String.format("[%s] Virtualhost %s removed", route[3], route[0]));
                            }
                            try {
                                version = Long.parseLong(route[3]);
                            } catch (NumberFormatException e) {
                                log.error(e.getMessage());
                            }
                        }
                    }
                }
            }
        });

        log.info(String.format("Instance %s started", this.toString()));
    }

    public long getVersion() {
        return this.version;
    }

    private Handler<HttpServerRequest> routeHandlerAction(final Action action) {
        final Logger log = this.getContainer().logger();
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                final String virtualHost = req.params() != null && req.params().contains("id") ? req.params().get("id") : "";
                req.bodyHandler(new Handler<Buffer>() {
                    public void handle(Buffer body) {
                        try {
                            JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost;
                            if (!"".equals(virtualHost)) {
                                jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                                if (!jsonVirtualHost.equalsIgnoreCase(virtualHost)) {
                                    throw new RuntimeException();
                                }
                            }
                            req.response().setStatusCode(200);
                            req.response().setStatusMessage("OK");
                            setRoute(json, action);
                        } catch (Exception e) {
                            log.error(String.format("routeHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            req.response().setStatusCode(400);
                            req.response().setStatusMessage("Bad Request");
                        } finally {
                            req.response().end();
                        }
                    }
                });
            }
        };
    }

    private void startHttpServer(final EventBus eb, final Logger log, final JsonObject serverConf) throws RuntimeException {

        RouteMatcher routeMatcher = new RouteMatcher();

        // FULL Infos/Changes
        routeMatcher.post("/route", routeHandlerAction(Action.ADD));
        routeMatcher.post("/route/:vh", routeHandlerAction(Action.ADD));

        routeMatcher.delete("/route", routeHandlerAction(Action.DEL));
        routeMatcher.delete("/route/:vh", routeHandlerAction(Action.DEL));

        routeMatcher.get("/route", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(200);
                req.response().setStatusMessage("OK");
                req.response().headers().set("Content-Type", "application/json");
                req.response().end(getRoutes().encodePrettily());
                log.info("GET /route");
            }
        });

        // Version (Only GET)
        routeMatcher.get("/version",new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(200);
                req.response().setStatusMessage("OK");
                req.response().headers().set("Content-Type", "application/json");
                JsonObject versionJson = new JsonObject(String.format("{\"version\":%d}", getVersion()));
                req.response().end(versionJson.encodePrettily());
                log.info(String.format("Version: %d", getVersion()));
                log.info("GET /version");
            }
        });

        // VirtualHost
        routeMatcher.post("/virtualhost", virtualhostHandlerAction(Action.ADD));

        routeMatcher.delete("/virtualhost", virtualhostHandlerAction(Action.DEL)); // ALL
        routeMatcher.delete("/virtualhost/:id", virtualhostHandlerAction(Action.DEL)); // Only ID

        routeMatcher.get("/virtualhost", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(200);
                req.response().setStatusMessage("OK");
                req.response().headers().set("Content-Type", "application/json");
                req.response().end(getVirtualHosts().encodePrettily());
                log.info("GET /virtualhost");
            }
        });

        routeMatcher.get("/virtualhost/:id", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                String virtualhost = req.params() != null && req.params().contains("id") ? req.params().get("id"): "";
                req.response().setStatusCode(200);
                req.response().setStatusMessage("OK");
                req.response().headers().set("Content-Type", "application/json");
                req.response().end(getVirtualHosts(virtualhost).encodePrettily());
                log.info(String.format("GET /virtualhost/%s", virtualhost));
            }
        });

        // Real (field "Virtualhost name" mandatory) - Only POST and DELETE
        routeMatcher.post("/real", realHandlerAction(eb, log, Action.ADD));
        routeMatcher.delete("/real/:id", realHandlerAction(eb, log, Action.DEL)); // Only with ID

        // Others methods/uris/etc
        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(400);
                req.response().setStatusMessage("Bad Request");
                req.response().headers().set("Content-Type", "application/json");
                req.response().end(new JsonObject("{\"status_message\":\"Not supported\"}").encodePrettily());
                log.warn(String.format("%s %s", req.method(), req.uri()));
            }
        });

        vertx.createHttpServer().requestHandler(routeMatcher).listen(serverConf.getInteger(CONF_PORT, 9090));
    }

    private Handler<HttpServerRequest> realHandlerAction(final EventBus eb, final Logger log, final Action action) {
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
                    public void handle(Buffer body) {
                        try {
                            final JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                            if (action==Action.DEL) {
                                JsonArray reals = json.containsField("endpoints") ? json.getArray("endpoints"): null;
                                if (reals!=null && reals.size() > 0) {
                                    if (!reals.get(0).equals(new JsonObject(String.format("{\"host\":\"%s\",\"port\":%s}", real, port)))) {
                                        throw new RuntimeException();
                                    }
                                } else {
                                    throw new RuntimeException();
                                }
                            }
                            if ("".equals(jsonVirtualHost)) {
                                throw new RuntimeException();
                            }
                            req.response().setStatusCode(200);
                            req.response().setStatusMessage("OK");
                            setRoute(json, action);
                        } catch (Exception e) {
                            log.error(String.format("realHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            req.response().setStatusCode(400);
                            req.response().setStatusMessage("Bad Request");
                        } finally {
                            req.response().end();
                        }
                    }
                });
            }
        };
    }

    protected JsonObject getVirtualHosts(String virtualhost) {
        if (!virtualhost.equals("")) {
            JsonArray routes = getRoutes().getArray("routes");
            Iterator<Object> it = routes.iterator();
            while (it.hasNext()) {
                JsonObject route = (JsonObject) it.next();
                if (route.getString("name").equalsIgnoreCase(virtualhost)) {
                    return route;
                }
            }
            return new JsonObject("{}");
        }
        return getRoutes();
    }

    private JsonObject getVirtualHosts() {
        return getVirtualHosts("");
    }

    private Handler<HttpServerRequest> virtualhostHandlerAction(final Action action) {
        final Logger log = this.getContainer().logger();
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                // TODO
            }
        };
    }

    public boolean existVirtualHost(String vhost) {
        return graphRoutes.containsKey(vhost);
    }

    public void setRoute(final JsonObject json, final Action action) throws RuntimeException {
        final Logger log = this.getContainer().logger();
        final EventBus eb = this.getVertx().eventBus();

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
            String host;
            Integer port;
            Long _version;
            JsonArray endpoints = null;
            JsonObject jsonTemp = (JsonObject) it.next();

            if (jsonTemp.containsField("name")) {
                vhost = jsonTemp.getString("name");
            } else {
                throw new RuntimeException("virtualhost undef");
            }
            if (jsonTemp.containsField("version")) {
                _version = jsonTemp.getLong("version");
            } else {
                throw new RuntimeException("version undef");
            }
            if (jsonTemp.containsField("endpoints")) {
                endpoints = jsonTemp.getArray("endpoints");
            } else {
                throw new RuntimeException("endpoints undef");
            }

            Iterator<Object> endpointsIterator = endpoints.iterator();
            while (endpointsIterator.hasNext()) {
                JsonObject endpointJson = (JsonObject) endpointsIterator.next();
                if (endpointJson.containsField("host")) {
                    host = endpointJson.getString("host");
                } else {
                    throw new RuntimeException("endpoint host undef");
                }
                if (endpointJson.containsField("port")) {
                    port = endpointJson.getInteger("port");
                } else {
                    throw new RuntimeException("endpoint port undef");
                }
                switch (action) {
                    case ADD:
                        eb.publish(QUEUE_ROUTE_ADD, String.format("%s:%s:%d:%d", vhost, host, port, _version));
                        break;
                    case DEL:
                        eb.publish(QUEUE_ROUTE_DEL, String.format("%s:%s:%d:%d", vhost, host, port, _version));
                        break;
                    default:
                        throw new RuntimeException("Action not supported");
                }
            }
        }
    }

    public JsonObject getRoutes() {
        JsonObject routes = new JsonObject();
        routes.putNumber("version", getVersion());
        JsonArray vhosts = new JsonArray();

        for (String vhost : graphRoutes.keySet()) {
            JsonObject vhostObj = new JsonObject();
            vhostObj.putString("name", vhost);
            JsonArray endpoints = new JsonArray();
            for (String value : graphRoutes.get(vhost)) {
                JsonObject endpointObj = new JsonObject();
                endpointObj.putString("host", value.split(":")[0]);
                endpointObj.putNumber("port", Integer.parseInt(value.split(":")[1]));
                endpoints.add(endpointObj);
            }
            vhostObj.putArray("endpoints", endpoints);
            vhosts.add(vhostObj);
        }
        routes.putArray("routes", vhosts);
        return routes;
    }
}
