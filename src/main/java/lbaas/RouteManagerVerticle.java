package lbaas;

import java.util.HashMap;
import java.util.HashSet;
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

        eb.registerHandler("route.add", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                final String[] route = message.body().split(separator.toString());
                if (route.length > 3) {
                    if (graphRoutes.containsKey(route[0])) {
                        final Set<String> clients = graphRoutes.get(route[0]);
                        if (clients !=null) {
                            clients.add(String.format("%s:%s", route[1], route[2]));
                        }
                    } else {
                        final Set<String> clients = new HashSet<String>();
                        graphRoutes.put(route[0], clients);
                        clients.add(String.format("%s:%s", route[1], route[2]));
                    }
                    try {
                        version = Long.parseLong(route[3]);
                    } catch (NumberFormatException e) {}
                }
            }
        });

        eb.registerHandler("route.del", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                final String[] route = message.body().split(separator.toString());
                if (route.length > 3) {
                    if (graphRoutes.containsKey(route[0])) {
                        final Set<String> clients = graphRoutes.get(route[0]);
                        if (clients != null) {
                            clients.remove(String.format("%s:%s", route[1], route[2]));
                        }
                    }
                    try {
                        version = Long.parseLong(route[3]);
                    } catch (NumberFormatException e) {}
                }
            }
        });

        log.info(String.format("Instance %s started", this.toString()));
    }

    public long getVersion() {
        return this.version;
    }

    private Handler<HttpServerRequest> routerMatcherHandlerAction(final EventBus eb, final Logger log, final Action action) {
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                req.bodyHandler(new Handler<Buffer>() {
                    public void handle(Buffer body) {
                        try {
                            JsonObject json = new JsonObject(body.toString());
                            req.response().setStatusCode(200);
                            req.response().setStatusMessage("OK");
                            setRoute(json, eb, action);
                            req.response().end();
                        } catch (Exception e) {
                            log.error(String.format("JSON FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            req.response().setStatusCode(400);
                            req.response().setStatusMessage("Bad Request");
                            req.response().end();
                        }
                    }
                });
            }
        };
    }

    private void startHttpServer(final EventBus eb, final Logger log, final JsonObject serverConf) throws RuntimeException {

        RouteMatcher routeMatcher = new RouteMatcher();
        routeMatcher.post("/route", routerMatcherHandlerAction(eb, log, Action.ADD));
        routeMatcher.delete("/route", routerMatcherHandlerAction(eb, log, Action.DEL));

        routeMatcher.get("/route", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(200);
                req.response().setStatusMessage("OK");
                req.response().headers().set("Content-Type", "application/json");
                req.response().end(getRoutes().encodePrettily());
            }
        });

        routeMatcher.get("/version",new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(200);
                req.response().setStatusMessage("OK");
                req.response().headers().set("Content-Type", "application/json");
                JsonObject versionJson = new JsonObject(String.format("{\"version\":%d}", getVersion()));
                req.response().end(versionJson.encodePrettily());
            }
        });

        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                req.response().setStatusCode(404);
                req.response().setStatusMessage("Not Found");
                req.response().end();
            }
        });

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(routeMatcher).listen(serverConf.getInteger("port", 9090));
    }

    public boolean existVirtualHost(String vhost) {
        return graphRoutes.containsKey(vhost);
    }

    public void setRoute(final JsonObject json, final EventBus eb, final Action action) throws RuntimeException {
        String vhost;
        String endpoint;
        Integer port;
        Long _version;
        if (json.containsField("vhost")) {
            vhost = json.getString("vhost");
        } else {
            throw new RuntimeException("virtualhost undef");
        }
        if (json.containsField("host")) {
            endpoint = json.getString("host");
        } else {
            throw new RuntimeException("endpoint host undef");
        }
        if (json.containsField("port")) {
            port = json.getInteger("port");
        } else {
            throw new RuntimeException("endpoint port undef");
        }
        if (json.containsField("version")) {
            _version = json.getLong("version");
        } else {
            throw new RuntimeException("version undef");
        }

        switch (action) {
            case ADD:
                eb.publish("route.add", String.format("%s:%s:%d:%d", vhost, endpoint, port, _version));
                break;
            case DEL:
                eb.publish("route.del", String.format("%s:%s:%d:%d", vhost, endpoint, port, _version));
                break;
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
