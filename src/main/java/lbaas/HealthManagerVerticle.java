package lbaas;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.QUEUE_HEALTHCHECK_OK;
import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

public class HealthManagerVerticle extends Verticle {

    private final Set<String> badClients = new HashSet<>();

    public void start() {
        final Logger log = container.logger();

        final JsonObject conf = container.config();
        // Milliseconds Interval
        final Long checkInterval = conf.getLong("checkInterval", 5000L);
        // 0L = Disable. Recommended 86400000 (1 day)
        final Long intervalForceAllOk = conf.getLong("intervalForceAllOk", 0L);
        // Default = "/". Recommended = "/health"
        final String uriHealthCheck = conf.getString("uriHealthCheck","/");

        final EventBus eb = vertx.eventBus();
        eb.registerHandler(QUEUE_HEALTHCHECK_OK, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                log.debug(String.format("Endpoint %s OK", message.body()));
                badClients.remove(message.body());
            };
        });
        eb.registerHandler(QUEUE_HEALTHCHECK_FAIL, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                log.error(String.format("Endpoint %s FAIL", message.body()));
                badClients.add(message.body());
            };
        });

        vertx.setPeriodic(checkInterval, new Handler<Long>() {
            @Override
            public void handle(Long timerID) {
                Iterator<String> it = badClients.iterator();
                while (it.hasNext()) {
                    String clientString = it.next();
                    final Client client = new Client(clientString, vertx);
                    client.setKeepAlive(false);
                    HttpClientRequest cReq = client.connect()
                            .request("GET", uriHealthCheck, new Handler<HttpClientResponse>() {
                        @Override
                        public void handle(HttpClientResponse cResp) {
                            if (cResp.statusCode()!=200) {
                                client.setHealthy(false);
                            }
                        }
                    });
                    cReq.exceptionHandler(new Handler<Throwable>(){
                        @Override
                        public void handle(Throwable event) {
                            client.setHealthy(false);
                        }
                    });
                    cReq.end();
                    if (client.isHealthy()) {
                        eb.publish(QUEUE_HEALTHCHECK_OK, clientString);
                    }
                    try {
                        client.close();
                    } catch (RuntimeException e) {} // Ignore double close
                }
            }
        });
        if (intervalForceAllOk!=0L) {
            vertx.setPeriodic(intervalForceAllOk, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    Iterator<String> it = badClients.iterator();
                    while (it.hasNext()) {
                        String clientString = it.next();
                        eb.publish(QUEUE_HEALTHCHECK_OK, clientString);
                    }
                    badClients.clear();
                }
            });
        }
        log.info(String.format("Instance %s started", this.toString()));
    }
}
