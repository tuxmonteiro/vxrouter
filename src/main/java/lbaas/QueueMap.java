/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
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
    private final Map<String, Set<Client>> badMap;

    public QueueMap(final Verticle verticle, final Map<String, Set<Client>> map, final Map<String, Set<Client>> badMap) {
        this.verticle = verticle;
        this.eb=(verticle != null) ? verticle.getVertx().eventBus() : null;
        this.log=(verticle != null) ? verticle.getContainer().logger() : null;
        this.map=map;
        this.badMap=badMap;
    }

    public boolean processAddMessage(String message) {
        boolean isOk = true;
        final String[] route = message.split(SEPARATOR.toString());
        if (route.length == NUM_FIELDS && map!=null && badMap!=null) {
            String virtualhost = route[0];
            String host = route[1];
            String port = route[2];
            boolean status = !route[3].equals("0");
            String uri = route[4];
            String endpoint = (!"".equals(host) && !"".equals(port)) ?
                    String.format("%s:%s", host, port) : "";
            String uriBase = uri.split("/")[1];
            final Map<String, Set<Client>> tempMap = status ? map : badMap;

            if (!status && !map.containsKey(virtualhost)) {
                log.warn(String.format("[%s] Endpoint %s failed do not created because Virtualhost %s not exist", verticle.toString(), endpoint, virtualhost));
                isOk = false;
                return isOk;
            }
            if (!tempMap.containsKey(virtualhost)) {
                tempMap.put(virtualhost, new HashSet<Client>());
                log.info(String.format("[%s] Virtualhost %s added", verticle.toString(), virtualhost));
                return isOk;
            } else {
                isOk = false;
            }
            if ("".equals(endpoint)) {
                return isOk;
            }
            if ("route".equals(uriBase)||"real".equals(uriBase)) {
                final Set<Client> clients = tempMap.get(virtualhost);
                Client client = new Client(endpoint, verticle.getVertx());
                if (clients.add(client)) {
                    log.info(String.format("[%s] Real %s (%s) added", verticle.toString(), endpoint, virtualhost));
                } else {
                    log.warn(String.format("[%s] Real %s (%s) already exist", verticle.toString(), endpoint, virtualhost));
                    isOk = false;
                }
            }
        }
        return isOk;
    }

    public boolean processDelMessage(String message) {
        boolean isOk = true;
        final String[] route = message.split(SEPARATOR.toString());
        if (route.length == NUM_FIELDS && map!=null && badMap!=null) {
            String virtualhost = route[0];
            String host = route[1];
            String port = route[2];
            boolean status = !route[3].equals("0");
            //String uri = route[4];
            String endpoint = (!"".equals(host) && !"".equals(port)) ?
                    String.format("%s:%s", host, port) : "";
            //String uriBase = uri.split("/")[1];
            final Map<String, Set<Client>> tempMap = status ? map : badMap;

            if ("".equals(endpoint)) {
                if (tempMap.containsKey(virtualhost)) {
                    tempMap.get(virtualhost).clear();
                    tempMap.remove(virtualhost);
                    log.info(String.format("[%s] Virtualhost %s removed", verticle.toString(), virtualhost));
                } else {
                    log.warn(String.format("[%s] Virtualhost not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                }
                return isOk;
            } else if (!tempMap.containsKey(virtualhost)) {
                log.warn(String.format("[%s] Real not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                isOk = false;
                return isOk;
            }
            final Set<Client> clients = tempMap.get(virtualhost);
            Client client = new Client(endpoint, verticle.getVertx());
            if (clients.remove(client)) {
                log.info(String.format("[%s] Real %s (%s) removed", verticle.toString(), endpoint, virtualhost));
            } else {
                if (status) {
                    final Set<Client> badClients = badMap.get(virtualhost);
                    if (badClients!=null) {
                        badClients.remove(client);
                        log.info(String.format("[%s] Real %s (%s) removed", verticle.toString(), endpoint, virtualhost));
                    } else {
                        isOk = false;
                    }
                } else {
                    log.warn(String.format("[%s] Real not removed. Real %s (%s) not exist", verticle.toString(), endpoint, virtualhost));
                    isOk = false;
                }
            }
        }
        return isOk;
    }

    public void processVersionMessage(String message) {
        try {
            setVersion(Long.parseLong(message));
        } catch (java.lang.NumberFormatException e) {}
    }

    public void registerQueueAdd() {
        Handler<Message<String>> queueAddHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processAddMessage(message.body());
                postAddEvent(message.body());
            }
        };
        if (eb != null) {
            eb.registerHandler(QUEUE_ROUTE_ADD, queueAddHandler);
        }
    }

    public void registerQueueDel() {
        Handler<Message<String>> queueDelHandler =  new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processDelMessage(message.body());
                postDelEvent(message.body());
            }
        };
        if (eb!=null) {
            eb.registerHandler(QUEUE_ROUTE_DEL,queueDelHandler);
        }
    }

    public void registerQueueVersion() {
        Handler<Message<String>> queueVersionHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processVersionMessage(message.body());
            }
        };
        if (eb!=null) {
            eb.registerHandler(QUEUE_ROUTE_VERSION, queueVersionHandler);
        }
    }

    private void setVersion(Long version) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).setVersion(version);
            log.info(String.format("[%s] POST /version: %d", verticle.toString(), version));
        }
    }

    private void postDelEvent(String message) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).postDelEvent(message);
        }
    }

    private void postAddEvent(String message) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).postAddEvent(message);
        }
    }

}
