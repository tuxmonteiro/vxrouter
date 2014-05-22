package lbaas;

import static lbaas.Constants.CONF_PORT;
import static lbaas.Constants.QUEUE_ROUTE_ADD;
import static lbaas.Constants.QUEUE_ROUTE_DEL;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class RouteManagerVerticle extends Verticle implements IQueueMapObserver {

    private final Map<String, Set<Client>> graphRoutes = new HashMap<>();
    private Long version = 0L;

    private enum Action {
        ADD,
        DEL
    }

    public void start() {
        final Logger log = container.logger();

        startHttpServer(container.config());
        final QueueMap queueMap = new QueueMap(this, graphRoutes);
        queueMap.registerQueueAdd();
        queueMap.registerQueueDel();
 
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
                            returnStatus(req,200);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("routeHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            returnStatus(req,400);
                        }
                    }
                });
            }
        };
    }

    private void startHttpServer(final JsonObject serverConf) throws RuntimeException {
        final EventBus eb = this.getVertx().eventBus();
        final Logger log = this.getContainer().logger();

        RouteMatcher routeMatcher = new RouteMatcher();

        // FULL Infos/Changes
        routeMatcher.post("/route", routeHandlerAction(Action.ADD));
        routeMatcher.post("/route/:vh", routeHandlerAction(Action.ADD));

        routeMatcher.get("/route", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                returnStatus(req, 200, getRoutes().encodePrettily());
                log.info("GET /route");
            }
        });

        // Version (Only GET)
        routeMatcher.get("/version",new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                JsonObject versionJson = new JsonObject(String.format("{\"version\":%d}", getVersion()));
                returnStatus(req, 200, versionJson.encodePrettily());
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
                returnStatus(req, 200, getVirtualHosts(""));
                log.info("GET /virtualhost");
            }
        });

        routeMatcher.get("/virtualhost/:id", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest req) {
                String virtualhost = req.params() != null && req.params().contains("id") ? req.params().get("id"): "";
                returnStatus(req, 200, getVirtualHosts(virtualhost));
                log.info(String.format("GET /virtualhost/%s", virtualhost));
            }
        });

        // Real (field "Virtualhost name" mandatory) - Only POST and DELETE
        routeMatcher.post("/real", realHandlerAction(eb, log, Action.ADD));
        routeMatcher.delete("/real/:id", realHandlerAction(eb, log, Action.DEL)); // Only with ID

        // Others methods/uris/etc
        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                returnStatus(req, 400);
                log.warn(String.format("%s %s not supported", req.method(), req.uri()));
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
                                if (reals!=null && !reals.toList().isEmpty() && !reals.get(0).equals(new JsonObject(String.format("{\"host\":\"%s\",\"port\":%s}", real, port)))) {
                                    throw new RuntimeException();
                                }
                            }
                            if ("".equals(jsonVirtualHost)) {
                                throw new RuntimeException();
                            }
                            returnStatus(req, 200);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("realHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            returnStatus(req, 400);
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
                String virtualhostRequest = "";
                try {
                    virtualhostRequest = req.params() != null && req.params().contains("id") ? 
                            java.net.URLDecoder.decode(req.params().get("id"), "UTF-8") : "";
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                final String virtualhost = virtualhostRequest;
                req.bodyHandler(new Handler<Buffer>() {
                    public void handle(Buffer body) {
                        try {
                            final JsonObject json = new JsonObject(body.toString());
                            String jsonVirtualHost = json.containsField("name") ? json.getString("name") : "";
                            if ("".equals(jsonVirtualHost)) {
                                throw new RuntimeException();
                            }
                            if (action==Action.DEL && !jsonVirtualHost.equals(virtualhost) && "".equals(virtualhost)) {
                                throw new RuntimeException();
                            }
                            returnStatus(req, 200);
                            setRoute(json, action, req.uri());
                        } catch (Exception e) {
                            log.error(String.format("virtualHostHandlerAction FAIL: %s\nBody: %s",
                                    e.getMessage(), body.toString()));
                            returnStatus(req, 400);
                        }
                    }
                });
            }
        };
    }

    public boolean existVirtualHost(String vhost) {
        return graphRoutes.containsKey(vhost);
    }

    public void setRoute(final JsonObject json, final Action action, final String uri) throws RuntimeException {
        Long myVersion;
        if (json.containsField("version")) {
            myVersion = json.getLong("version");
        } else {
            throw new RuntimeException("version undef");
        }

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
            JsonArray endpoints = null;
            JsonObject jsonTemp = (JsonObject) it.next();

            if (jsonTemp.containsField("name")) {
                vhost = jsonTemp.getString("name");
            } else {
                throw new RuntimeException("virtualhost undef");
            }

            if (jsonTemp.containsField("endpoints")) {
                endpoints = jsonTemp.getArray("endpoints");
                Iterator<Object> endpointsIterator = endpoints.iterator();
                while (endpointsIterator.hasNext()) {
                    JsonObject endpointJson = (JsonObject) endpointsIterator.next();
                    host = endpointJson.containsField("host") ? endpointJson.getString("host"):"";
                    port = endpointJson.containsField("port") ? endpointJson.getInteger("port"):null;
                    if ("".equals(host) || port==null) {
                        throw new RuntimeException("Endpoint host or port undef");
                    }
                    String message = String.format("%s:%s:%d:%d:%s", vhost, host, port, myVersion, uri);
                    sendAction(message, action);
                }
            } else {
                String message = String.format("%s:::%d:%s", vhost, myVersion, uri);
                sendAction(message, action);
            }

        }
    }

    public void sendAction(String message, Action action) {
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
            default:
                throw new RuntimeException("Action not supported");
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
            for (Client value : graphRoutes.get(vhost)) {
                JsonObject endpointObj = new JsonObject();
                endpointObj.putString("host", value.toString().split(":")[0]);
                endpointObj.putNumber("port", Integer.parseInt(value.toString().split(":")[1]));
                endpoints.add(endpointObj);
            }
            vhostObj.putArray("endpoints", endpoints);
            vhosts.add(vhostObj);
        }
        routes.putArray("routes", vhosts);
        return routes;
    }

    private void returnStatus(final HttpServerRequest req, Integer code) {
        returnStatus(req, code, "");
    }

    private void returnStatus(final HttpServerRequest req, Integer code, String message) {
        req.response().setStatusCode(code);
        req.response().setStatusMessage(HttpResponseStatus.valueOf(code).reasonPhrase());
        req.response().headers().set("Content-Type", "application/json");
        String messageReturn = message;
        if ("".equals(message)) {
            JsonObject json = new JsonObject(String.format("{ \"status_message\":\"%s\"}", req.response().getStatusMessage()));
            messageReturn = json.encodePrettily();
        }
        req.response().end(messageReturn);
    }

    @Override
    public void setVersion(Long version) {
        this.version = version;
    }
}
