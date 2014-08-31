package lbaas.core;

import static lbaas.core.Constants.QUEUE_BACKEND_CONNECTIONS_PREFIX;
import static lbaas.core.Constants.numConnectionFieldName;
import static lbaas.core.Constants.uuidFieldName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class BackendSessionController {

    private final Vertx vertx;
    private final EventBus eb;

    private Long schedulerId = 0L;
    private Long schedulerDelay = 10000L;
    private Long connectionMapTimeout = 60000L;

    // < remoteWithPort, timestamp >
    private final Map<String, Long> connections = new HashMap<>();
    // < backendInstanceUUID, numConnections >
    private final Map<String, Integer> globalConnections = new HashMap<>();

    private final String queueActiveConnections;
    private final String myUUID;
    private boolean registered = false;

    private boolean newConnection = true;
    private int activeConnections = 0;

    public BackendSessionController(final String backendWithPort, final Vertx vertx) {
        this.vertx = vertx;
        this.eb = (vertx!=null) ? vertx.eventBus() : null;
        this.queueActiveConnections = String.format("%s%s", QUEUE_BACKEND_CONNECTIONS_PREFIX, backendWithPort);
        this.myUUID = UUID.randomUUID().toString();
    }

    private JsonObject zeroConnectionJson() {
        JsonObject myConnections = new JsonObject();
        myConnections.putString(uuidFieldName, myUUID);
        myConnections.putNumber(numConnectionFieldName, 0);
        return myConnections;
    }

    public void initEventBus() {
        eb.publish(queueActiveConnections, zeroConnectionJson());
    }

    public void registerEventBus() {
        if (!registered && eb!=null) {
            eb.registerLocalHandler(queueActiveConnections, getHandlerListenGlobalConnections());
            registered = true;
        }
    }

    private Handler<Message<JsonObject>> getHandlerListenGlobalConnections() {
        return new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject messageJson = message.body();
                String uuid = messageJson.getString(uuidFieldName);
                if (uuid != myUUID) {
                    int numConnections = messageJson.getInteger(numConnectionFieldName);
                    globalConnections.put(uuid, numConnections);
                }
            }
        };
    }

    public boolean addConnection(String connectionId) {
        newConnection = connections.put(connectionId, System.currentTimeMillis()) == null;
        activeScheduler();
        return newConnection;
    }

    public boolean addConnection(String host, String port) {
        String connectionId = String.format("%s:%s", host, port);
        return addConnection(connectionId);
    }

    public boolean removeConnection(String connectionId) {
        return connections.remove(connectionId) != null;
    }

    public void unregisterEventBus() {
        if (eb!=null) {
            eb.publish(queueActiveConnections, zeroConnectionJson());
            if (registered) {
                eb.unregisterHandler(queueActiveConnections, getHandlerListenGlobalConnections());
                registered = false;
            }
        }
    }

    public void clearConnectionsMap() {
        connections.clear();
        globalConnections.clear();
        cancelScheduler();
    }

    public Integer getActiveConnections() {
        int counterActiveConnection = (activeConnections > 0) ? activeConnections : recalcNumConnections();
        return counterActiveConnection;
    }

    public Integer getInstanceActiveConnections() {
        return connections.size();
    }

    public boolean isNewConenction(String remoteId) {
        return newConnection;
    }

    public boolean isNewConnection(String remoteIP, String remotePort) {
        String remoteId = String.format("%s:%s", remoteIP, remotePort);
        return isNewConenction(remoteId);
    }

    public Long getSchedulerDelay() {
        return schedulerDelay;
    }

    public BackendSessionController setSchedulerDelay(Long schedulerDelay) {
        if (this.schedulerDelay!=schedulerDelay) {
            this.schedulerDelay = schedulerDelay;
            cancelScheduler();
            activeScheduler();
        }
        return this;
    }

    public Long getConnectionMapTimeout() {
        return connectionMapTimeout;
    }

    public BackendSessionController setConnectionMapTimeout(Long connectionMapTimeout) {
        this.connectionMapTimeout = connectionMapTimeout;
        return this;
    }

    private void expireLocalConnections() {
        Long timeout = System.currentTimeMillis() - connectionMapTimeout;
        Set<String> connectionIds = new HashSet<>(connections.keySet());
        for (String remote : connectionIds) {
            if (connections.get(remote)<timeout) {
                removeConnection(remote);
            }
        }
    }

    private void clearGlobalConnections() {
        globalConnections.clear();
    }

    private void notifyNumConnections() {
        Integer localConnections = getInstanceActiveConnections();
        if (localConnections>0) {
            JsonObject myConnections = new JsonObject();
            myConnections.putString(uuidFieldName, myUUID);
            myConnections.putNumber(numConnectionFieldName, localConnections);
            eb.publish(queueActiveConnections, myConnections);
        }
    }

    private int recalcNumConnections() {
        int globalSum = getInstanceActiveConnections();
        for (int externalValue: globalConnections.values()) {
            globalSum =+ externalValue;
        }
        activeConnections = globalSum;
        return activeConnections;
    }

    public void activeScheduler() {
        if (schedulerId==0L && vertx!=null) {
            schedulerId = vertx.setPeriodic(schedulerDelay, new Handler<Long>() {

                @Override
                public void handle(Long event) {
                    expireLocalConnections();
                    recalcNumConnections();
                    clearGlobalConnections();
                    notifyNumConnections();
                }
            });
        }
    }

    public void cancelScheduler() {
        if (schedulerId!=0L && vertx!=null) {
            boolean canceled = vertx.cancelTimer(schedulerId);
            if (canceled) {
                schedulerId=0L;
            }
        }
    }

}
