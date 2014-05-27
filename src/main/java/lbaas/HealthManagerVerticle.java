package lbaas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.QUEUE_HEALTHCHECK_OK;
import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

public class HealthManagerVerticle extends Verticle {

    final Map<String, Set<String>> badEndpointMap = new HashMap<>();

    private String routeManagerHost;
    private Integer routeManagerPort;

    public void start() {
        final Logger log = container.logger();

        final JsonObject conf = container.config();
        // Milliseconds Interval
        final Long checkInterval = conf.getLong("checkInterval", 5000L);
//TODO        // 0L = Disable. Recommended 86400000 (1 day)
//TODO        final Long intervalForceAllOk = conf.getLong("intervalForceAllOk", 0L);
        // Default = "/". Recommended = "/health"
        final String uriHealthCheck = conf.getString("uriHealthCheck","/");

        routeManagerHost = conf.getString("routeManagerHost","127.0.0.1");
        routeManagerPort = conf.getInteger("routeManagerPort", 9090);

        final EventBus eb = vertx.eventBus();
        eb.registerHandler(QUEUE_HEALTHCHECK_OK, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String endpoint = message.body();
                moveEndpoint(endpoint, true);
                log.debug(String.format("Endpoint %s OK", message.body()));
            };
        });
        eb.registerHandler(QUEUE_HEALTHCHECK_FAIL, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String endpoint = message.body();
                moveEndpoint(endpoint, false);
                log.error(String.format("Endpoint %s FAIL", endpoint));
            };
        });

        vertx.setPeriodic(checkInterval, new Handler<Long>() {
            @Override
            public void handle(Long timerID) {
                vertx.createHttpClient()
                    .setHost(routeManagerHost)
                    .setPort(routeManagerPort)
                    .getNow("/route", new Handler<HttpClientResponse>() {
                        @Override
                        public void handle(HttpClientResponse cResp) {
                            cResp.bodyHandler(new Handler<Buffer>() {
                                @Override
                                public void handle(Buffer buffer) {
                                    JsonObject resp = new JsonObject(buffer.toString());
                                    JsonArray routes = resp.getArray("routes");
                                    Iterator<Object> it = routes.iterator();
                                    badEndpointMap.clear();
                                    while (it.hasNext()) {
                                        JsonObject route = (JsonObject) it.next();
                                        String virtualhost = route.getString("name");
                                        JsonArray badEndpoints = route.getArray("badEndpoints");
                                        Iterator<Object> it2 = badEndpoints.iterator();
                                        while (it2.hasNext()) {
                                            JsonObject endpointJson = (JsonObject) it.next();
                                            String host = endpointJson.getString("host");
                                            Integer port = endpointJson.getInteger("port");
                                            String endpoint = String.format("%s:%d", host, port);
                                            if (!badEndpointMap.containsKey(endpoint)) {
                                                badEndpointMap.put(endpoint, new HashSet<String>());
                                            }
                                            final Set<String> virtualhosts = badEndpointMap.get(endpoint);
                                            virtualhosts.add(virtualhost);
                                        }
                                    }
                                    Iterator<String> it3 = badEndpointMap.keySet().iterator();
                                    while (it3.hasNext()) {
                                        final String endpoint = it3.next();
                                        Client client = new Client(endpoint, vertx);
                                        client.setKeepAlive(false);
                                        HttpClient httpClient = client.connect();
                                        httpClient.getNow(uriHealthCheck, new Handler<HttpClientResponse>() {
                                            @Override
                                            public void handle(HttpClientResponse cResp) {
                                                if (cResp.statusCode()==200) {
                                                    eb.publish(QUEUE_HEALTHCHECK_OK, endpoint);
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
            }
        });
//        if (intervalForceAllOk!=0L) {
//            vertx.setPeriodic(intervalForceAllOk, new Handler<Long>() {
//                @Override
//                public void handle(Long event) {
//                    Iterator<String> it = badClients.iterator();
//                    while (it.hasNext()) {
//                        String clientString = it.next();
//                        eb.publish(QUEUE_HEALTHCHECK_OK, clientString);
//                    }
//                    badClients.clear();
//                }
//            });
//        }
        log.info(String.format("Instance %s started", this.toString()));
    }

    private void moveEndpoint(final String endpoint, final Boolean status) {
        Set <String> virtualhosts = badEndpointMap.get(endpoint);
        Iterator<String> it = virtualhosts.iterator();
        while (it.hasNext()) {
            String virtualhost = it.next();
            String[] endpointArray = endpoint.split(":");
            String prefix = String.format("{\"name\": \"%s\", \"endpoints\":[{\"host\":\"%s\", \"port\": %d", virtualhost, endpointArray[0], endpointArray[1]);
            JsonObject jsonRemove = new JsonObject(String.format("%s%s}]}", prefix, status ? ",\"status\":false" : ""));
            JsonObject jsonAdd = new JsonObject(String.format("%s%s}]}", prefix, !status ? ",\"status\":false": ""));

            vertx.createHttpClient()
                .setHost(routeManagerHost)
                .setPort(routeManagerPort)
                .setKeepAlive(true)
                .delete("/real", null).write(jsonRemove.toString());
            vertx.createHttpClient()
                .setHost(routeManagerHost)
                .setPort(routeManagerPort)
                .setKeepAlive(true)
                .post("/real", null).write(jsonAdd.toString());
        }

    }
}
