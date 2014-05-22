package lbaas;

import static lbaas.Constants.QUEUE_ROUTE_ADD;
import static lbaas.Constants.QUEUE_ROUTE_DEL;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.SEPARATOR;

public class QueueMap {

    private final Verticle verticle;
    private final EventBus eb;
    private final Logger log;
    private final Map<String, Set<Client>> map;

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
                if (route.length > 4) {
                    String virtualhost = route[0];
                    String host = route[1];
                    String port = route[2];
                    Long version = Long.parseLong(route[3]);
                    String uri = route[4];
                    String endpoint = (!"".equals(host) && !"".equals(port)) ?
                            String.format("%s:%s", host, port) : "";
                    String uriBase = uri.split("/")[1];

                    if ("route".equals(uriBase) || "virtualhost".equals(uriBase) && !map.containsKey(virtualhost)) {
                        map.put(virtualhost, new HashSet<Client>());
                        log.info(String.format("[%d] Virtualhost %s added", version, virtualhost));
                        setVersion(version);
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
                            log.info(String.format("[%d] Real %s (%s) added", version, endpoint, virtualhost));
                            setVersion(version);
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
                if (route.length > 3) {
                    String virtualhost = route[0];
                    String host = route[1];
                    String port = route[2];
                    Long version = Long.parseLong(route[3]);
//                    String uri = route[4];
                    String endpoint = (!"".equals(host) && !"".equals(port)) ?
                            String.format("%s:%s", host, port) : "";
//                    String uriBase = uri.split("/")[1];

                    if ("".equals(endpoint)) {
                        if (map.containsKey(virtualhost)) {
                            map.get(virtualhost).clear();
                            map.remove(virtualhost);
                            log.info(String.format("[%d] Virtualhost %s removed", version, virtualhost));
                            setVersion(version);
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
                        log.info(String.format("[%d] Real %s (%s) removed", version, endpoint, virtualhost));
                        setVersion(version);
                    } else {
                        log.warn(String.format("Real not removed. Real %s (%s) not exist", endpoint, virtualhost));
                    }
                }
            }
        });
    }

    public void setVersion(Long version) {
        try {
            ((IQueueMapObserver)verticle).setVersion(version);
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
    }
}
