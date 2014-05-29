package lbaas;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.NUM_FIELDS;
import static lbaas.Constants.QUEUE_HEALTHCHECK_OK;
import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;
import static lbaas.Constants.QUEUE_ROUTE_DEL;
import static lbaas.Constants.QUEUE_ROUTE_ADD;
import static lbaas.Constants.SEPARATOR;

public class HealthManagerVerticle extends Verticle implements IEventObserver {

    private final Map<String, Set<String>> endPointsMap = new HashMap<>();
    private final Map<String, Set<String>> badEndPointsMap = new HashMap<>();

    public void start() {
        final Logger log = container.logger();

        final JsonObject conf = container.config();
        final Long checkInterval = conf.getLong("checkInterval", 5000L); // Milliseconds Interval
        final String uriHealthCheck = conf.getString("uriHealthCheck","/"); // Recommended = "/health"

        final QueueMap queueMap = new QueueMap(this, null, null);
        queueMap.registerQueueAdd();
        queueMap.registerQueueDel();

        final EventBus eb = vertx.eventBus();
        eb.registerHandler(QUEUE_HEALTHCHECK_OK, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String endpoint = message.body();
                try {
                    moveEndpoint(endpoint, true);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                log.debug(String.format("Endpoint %s OK", message.body()));
            };
        });
        eb.registerHandler(QUEUE_HEALTHCHECK_FAIL, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String endpoint = message.body();
                try {
                    moveEndpoint(endpoint, false);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                log.error(String.format("Endpoint %s FAIL", endpoint));
            };
        });

        vertx.setPeriodic(checkInterval, new Handler<Long>() {
            @Override
            public void handle(Long timerID) {
                log.info("Checking bad endpoints...");
                if (badEndPointsMap!=null) {
                    Iterator<String> it = badEndPointsMap.keySet().iterator();
                    while (it.hasNext()) {
                        final String endpoint = it.next();
                        String[] hostWithPort = endpoint.split(":");
                        String host = hostWithPort[0];
                        Integer port = Integer.parseInt(hostWithPort[1]);
                        try {
                            HttpClient client = vertx.createHttpClient()
                                .setHost(host)
                                .setPort(port)
                                .exceptionHandler(new Handler<Throwable>() {
                                    @Override
                                    public void handle(Throwable event) {}
                                });
                            HttpClientRequest cReq = client.get(uriHealthCheck, new Handler<HttpClientResponse>() {
                                    @Override
                                    public void handle(HttpClientResponse cResp) {
                                        if (cResp!=null && cResp.statusCode()==200) {
                                            eb.publish(QUEUE_HEALTHCHECK_OK, endpoint);
                                            log.info(String.format("Real %s OK. Enabling it", endpoint));
                                        }
                                    }
                                });
                            cReq.headers().set("Host", (String) badEndPointsMap.get(endpoint).toArray()[0]);
                            cReq.exceptionHandler(new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable event) {}
                            });
                            cReq.end();
                        } catch (Exception e) {}
                    }
                }
            }
        });
        log.info(String.format("Instance %s started", this.toString()));
    }

    private void moveEndpoint(final String endpoint, final Boolean status) throws UnsupportedEncodingException {

        final EventBus eb = this.getVertx().eventBus();

        Set<String> virtualhosts = status ? badEndPointsMap.get(endpoint) : endPointsMap.get(endpoint);

        if (virtualhosts!=null) {
            Iterator<String> it = virtualhosts.iterator();
            while (it.hasNext()) {
                String message;
                String virtualhost = it.next();
                String[] endpointArray = endpoint.split(":");
                String host = endpointArray[0];
                String port = endpointArray[1];

                message = String.format("%s:%s:%s:%d:%s", virtualhost, host, port, status ? 0 : 1, String.format("/real/%s", URLEncoder.encode(endpoint,"UTF-8")));
                eb.publish(QUEUE_ROUTE_DEL, message);

                message = String.format("%s:%s:%s:%d:%s", virtualhost, host, port, status ? 1 : 0, "/real");
                eb.publish(QUEUE_ROUTE_ADD, message);
            }
        }
    }

    private void messageToMap(final String message, final Map<String, String> map) {
        final String[] route = message.split(SEPARATOR.toString());
        if (route.length == NUM_FIELDS && map!=null) {
            map.put("virtualhost", route[0]);
            map.put("host",route[1]);
            map.put("port",route[2]);
            map.put("status",!route[3].equals("0") ? "true":"false");
            map.put("uri",route[4]);
            map.put("endpoint",(!"".equals(route[1]) && !"".equals(route[2])) ? String.format("%s:%s", route[1], route[2]) : "");
            map.put("uriBase", route[4].contains("/")?route[4].split("/")[1]:"");
        }
    }

    @Override
    public void setVersion(Long version) {}

    @Override
    public void postAddEvent(String message) {
        Map<String, String> map = new HashMap<>();
        messageToMap(message, map);
        final Map <String, Set<String>> tempMap = "true".equals(map.get("status")) ? endPointsMap : badEndPointsMap;

        if (!tempMap.containsKey(map.get("endpoint"))) {
            tempMap.put(map.get("endpoint"), new HashSet<String>());
        }
        Set<String> virtualhosts = tempMap.get(map.get("endpoint"));
        virtualhosts.add(map.get("virtualhost"));
    };

    @Override
    public void postDelEvent(String message) {
        Map<String, String> map = new HashMap<>();
        messageToMap(message, map);
        final Map <String, Set<String>> tempMap = "true".equals(map.get("status")) ? endPointsMap : badEndPointsMap;

        if (tempMap.containsKey(map.get("endpoint"))) {
            Set<String> virtualhosts = tempMap.get(map.get("endpoint"));
            virtualhosts.remove(map.get("virtualhost"));
            if (virtualhosts.isEmpty()) {
                tempMap.remove(map.get("endpoint"));
            }
        }
    };
}
