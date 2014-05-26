package lbaas;

import static lbaas.Constants.QUEUE_ROUTE_ADD;
import static lbaas.Constants.QUEUE_ROUTE_DEL;
import static lbaas.Constants.QUEUE_ROUTE_VERSION;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.SEPARATOR;
import static lbaas.Constants.NUM_FIELDS;

public class QueueMap {

    private final Verticle verticle;
    private final EventBus eb;
    private final Logger log;
    private final Map<String, Set<Client>> map;
    private Long version;

    public QueueMap(final Verticle verticle, final Map<String, Set<Client>> map) {
        this.verticle = verticle;
        this.eb=verticle.getVertx().eventBus();
        this.log=verticle.getContainer().logger();
        this.map=map;
    }

    public void registerQueueAdd() {
        eb.registerHandler(QUEUE_ROUTE_ADD, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                final String[] route = message.body().split(SEPARATOR.toString());
                if (route.length == NUM_FIELDS) {
                    String virtualhost = route[0];
                    String host = route[1];
                    String port = route[2];
                    String uri = route[3];
                    String endpoint = (!"".equals(host) && !"".equals(port)) ?
                            String.format("%s:%s", host, port) : "";
                    String uriBase = uri.split("/")[1];

                    if ("route".equals(uriBase) || "virtualhost".equals(uriBase) && !map.containsKey(virtualhost)) {
                        map.put(virtualhost, new HashSet<Client>());
                        log.info(String.format("Virtualhost %s added",virtualhost));
                    }
                    if ("".equals(endpoint)) {
                        return;
                    } else if (!map.containsKey(virtualhost)) {
                        log.warn(String.format("Virtualhost %s not exist", virtualhost));
                        return;
                    }
                    if ("route".equals(uriBase)||"real".equals(uriBase)) {
                        final Set<Client> clients = map.get(virtualhost);
                        Client client = new Client(endpoint, verticle.getVertx());
                        if (clients.add(client)) {
                            log.info(String.format("Real %s (%s) added", endpoint, virtualhost));
                        } else {
                            log.warn(String.format("Real %s (%s) already exist", endpoint, virtualhost));
                        }
                    }
                }
            }
        });
    }
    public void registerQueueDel() {
        eb.registerHandler(QUEUE_ROUTE_DEL, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                final String[] route = message.body().split(SEPARATOR.toString());
                if (route.length == NUM_FIELDS) {
                    String virtualhost = route[0];
                    String host = route[1];
                    String port = route[2];
//                    String uri = route[3];
                    String endpoint = (!"".equals(host) && !"".equals(port)) ?
                            String.format("%s:%s", host, port) : "";
//                    String uriBase = uri.split("/")[1];

                    if ("".equals(endpoint)) {
                        if (map.containsKey(virtualhost)) {
                            map.get(virtualhost).clear();
                            map.remove(virtualhost);
                            log.info(String.format("Virtualhost %s removed", virtualhost));
                        } else {
                            log.warn(String.format("Virtualhost not removed. Virtualhost %s not exist", virtualhost));
                        }
                        return;
                    } else if (!map.containsKey(virtualhost)) {
                        log.warn(String.format("Real not removed. Virtualhost %s not exist", virtualhost));
                        return;
                    }
                    final Set<Client> clients = map.get(virtualhost);
                    if (clients.remove(new Client(endpoint, verticle.getVertx()))) {
                        log.info(String.format("Real %s (%s) removed", endpoint, virtualhost));
                    } else {
                        log.warn(String.format("Real not removed. Real %s (%s) not exist", endpoint, virtualhost));
                    }
                }
            }
        });
    }

    public void registerQueueVersion() {
        eb.registerHandler(QUEUE_ROUTE_VERSION, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                try {
                    System.out.println(message.body().toString());
                    setVersion(Long.parseLong(message.body().toString()));
                } catch (java.lang.NumberFormatException e) {}
            }
        });
    }

    public void setVersion(Long version) {
        ((IVersionObserver)verticle).setVersion(version);
        log.info(String.format("POST /version: %d", version));
    }

}
