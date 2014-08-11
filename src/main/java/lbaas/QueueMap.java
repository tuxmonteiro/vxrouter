/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import static lbaas.Constants.QUEUE_ROUTE_ADD;
import static lbaas.Constants.QUEUE_ROUTE_DEL;
import static lbaas.Constants.QUEUE_ROUTE_VERSION;
import java.util.Iterator;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class QueueMap {

    private final Verticle verticle;
    private final Vertx vertx;
    private final EventBus eb;
    private final Logger log;
    private final Map<String, Virtualhost> virtualhosts;

    public static String buildMessage(String virtualhostStr,
                                      String hostStr,
                                      String portStr,
                                      String statusStr,
                                      String uriStr,
                                      String properties)
    {
        JsonObject messageJson = new JsonObject();
        messageJson.putString("virtualhost", virtualhostStr);
        messageJson.putString("host", hostStr);
        messageJson.putString("port", portStr);
        messageJson.putString("status", statusStr);
        messageJson.putString("uri", uriStr);
        messageJson.putString("properties", properties);

        return messageJson.toString();
    }

    public QueueMap(final Verticle verticle, final Map<String, Virtualhost> virtualhosts) {
        this.verticle = verticle;
        this.vertx = verticle.getVertx();
        this.eb=(verticle != null) ? verticle.getVertx().eventBus() : null;
        this.log=(verticle != null) ? verticle.getContainer().logger() : null;
        this.virtualhosts=virtualhosts;
    }

    public boolean processAddMessage(String message) {
        if (virtualhosts==null) {
            return false;
        }

        boolean isOk = true;
        JsonObject messageJson = new JsonObject(message);
        String virtualhost = messageJson.getString("virtualhost", "");
        String uri = messageJson.getString("uri", "");
        String uriBase = uri.split("/")[1];

        switch (uriBase) {
            case "route":
            case "virtualhost":
                if (!virtualhosts.containsKey(virtualhost)) {
                    String properties = messageJson.getString("properties", "{}");

                    Virtualhost virtualhostObj = new Virtualhost(virtualhost, vertx);
                    try {
                        if (!"".equals(properties)) {
                            JsonObject propertiesJson = new JsonObject(properties);
                            virtualhostObj.mergeIn(propertiesJson.getObject("properties", new JsonObject()));
                        }
                    } catch (DecodeException e1) {
                        log.error(String.format("[%s] Properties decode failed (%s): %s", verticle.toString(), virtualhost, properties));
                    } catch (Exception e2) {
                        log.error(String.format("[%s] %s:\n%s", verticle.toString(), e2.getMessage(), e2.getStackTrace()));
                    }
                    virtualhosts.put(virtualhost, virtualhostObj);
                    log.info(String.format("[%s] Virtualhost %s added", verticle.toString(), virtualhost));
                    isOk = true;
                } else {
                    isOk = false;
                }
                break;
            case "real":
                if (!virtualhosts.containsKey(virtualhost)) {
                    log.warn(String.format("[%s] Endpoint didnt create, because Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                } else {

                    String host = messageJson.getString("host", "");
                    String port = messageJson.getString("port", "");
                    boolean status = !"0".equals(messageJson.getString("status", ""));
                    String endpoint = (!"".equals(host) && !"".equals(port)) ?
                            String.format("%s:%s", host, port) : "";

                    final Virtualhost vhost = virtualhosts.get(virtualhost);
                    if (vhost.addClient(endpoint, status)) {
                        log.info(String.format("[%s] Real %s (%s) added", verticle.toString(), endpoint, virtualhost));
                    } else {
                        log.warn(String.format("[%s] Real %s (%s) already exist", verticle.toString(), endpoint, virtualhost));
                        isOk = false;
                    }
                }
                break;
            default:
                log.warn(String.format("[%s] uriBase %s not supported", verticle.toString(), uriBase));
                isOk = false;
                break;
        }
        return isOk;
    }

    public boolean processDelMessage(String message) {
        if (virtualhosts==null) {
            return false;
        }

        boolean isOk = true;
        JsonObject messageJson = new JsonObject(message);
        String virtualhost = messageJson.getString("virtualhost", "");
        String host = messageJson.getString("host", "");
        String port = messageJson.getString("port");
        boolean status = !"0".equals(messageJson.getString("status", ""));
        String uri = messageJson.getString("uri", "");
//        String properties = messageJson.getString("properties", "{}");

        String endpoint = (!"".equals(host) && !"".equals(port)) ?
                String.format("%s:%s", host, port) : "";
        String uriBase = uri.split("/")[1];

        switch (uriBase) {
            case "route":
                Iterator<Virtualhost> iterVirtualhost = virtualhosts.values().iterator();
                while (iterVirtualhost.hasNext()) {
                    Virtualhost aVirtualhost = iterVirtualhost.next();
                    if (aVirtualhost!=null) {
                        aVirtualhost.clear(true);
                        aVirtualhost.clear(false);
                    }
                }
                virtualhosts.clear();
                log.info(String.format("[%s] All routes were cleaned", verticle.toString()));
                break;
            case "virtualhost":
                if (virtualhosts.containsKey(virtualhost)) {
                    virtualhosts.get(virtualhost).clearAll();
                    virtualhosts.remove(virtualhost);
                    log.info(String.format("[%s] Virtualhost %s removed", verticle.toString(), virtualhost));
                } else {
                    log.warn(String.format("[%s] Virtualhost not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                }
                break;
            case "real":
                if ("".equals(endpoint)) {
                    log.warn(String.format("[%s] Real UNDEF", verticle.toString(), endpoint));
                    isOk = false;
                } else if (!virtualhosts.containsKey(virtualhost)) {
                    log.warn(String.format("[%s] Real not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                } else {
                    final Virtualhost virtualhostObj = virtualhosts.get(virtualhost);
                    if (virtualhostObj!=null && virtualhostObj.removeClient(endpoint, status)) {
                        log.info(String.format("[%s] Real %s (%s) removed", verticle.toString(), endpoint, virtualhost));
                    } else {
                        log.warn(String.format("[%s] Real not removed. Real %s (%s) not exist", verticle.toString(), endpoint, virtualhost));
                        isOk = false;
                    }
                }
                break;
            default:
                log.warn(String.format("[%s] uriBase %s not supported", verticle.toString(), uriBase));
                isOk = false;
                break;
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
