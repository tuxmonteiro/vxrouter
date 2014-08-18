/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package lbaas;

import static lbaas.Constants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

public class Backend {

    private final Vertx vertx;
    private final EventBus eb;

    private HttpClient client;

    private final String host;
    private final Integer port;
    private Integer connectionTimeout;
    private boolean keepalive;
    private Long keepAliveMaxRequest;
    private Long keepAliveTimeOut;
    private int backendMaxPoolSize;

    private Long keepAliveTimeMark;
    private Long requestCount;

    private Long schedulerId = 0L;
    private Long schedulerDelay = 10000L;

    // < remoteWithPort, timestamp >
    private final Map<String, Long> connections = new HashMap<>();
    // < backendInstanceUUID, numConnections >
    private final Map<String, Integer> globalConnections = new HashMap<>();

    private final String queueActiveConnections;
    private final String myUUID;
    private boolean registered = false;

    @Override
    public String toString() {
        return String.format("%s:%d", this.host, this.port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Backend other = (Backend) obj;
        if (host == null) {
            if (other.host != null) return false;
        } else
            if (!host.equalsIgnoreCase(other.host)) return false;
        if (port == null) {
            if (other.port != null) return false;
        } else
            if (!port.equals(other.port)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public Backend(final String hostWithPort, final Vertx vertx) {
        String[] hostWithPortArray = hostWithPort!=null ? hostWithPort.split(":") : null;
        this.vertx = vertx;
        this.eb = (vertx!=null) ? vertx.eventBus() : null;
        this.client = null;
        if (hostWithPortArray != null && hostWithPortArray.length>1) {
            this.host = hostWithPortArray[0];
            this.port = Integer.parseInt(hostWithPortArray[1]);
        } else {
            this.host = null;
            this.port = null;
        }
        this.connectionTimeout = 60000;
        this.keepalive = true;
        this.keepAliveMaxRequest = Long.MAX_VALUE-1;
        this.keepAliveTimeMark = System.currentTimeMillis();
        this.keepAliveTimeOut = 86400000L; // One day
        this.requestCount = 0L;
        this.queueActiveConnections = String.format("%s%s", QUEUE_BACKEND_CONNECTIONS_PREFIX, this);
        this.myUUID = UUID.randomUUID().toString();

    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Backend setConnectionTimeout(Integer timeout) {
        this.connectionTimeout = timeout;
        return this;
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    public Backend setKeepAlive(boolean keepalive) {
        this.keepalive = keepalive;
        return this;
    }

    public Long getKeepAliveMaxRequest() {
      return keepAliveMaxRequest;
    }

    public Backend setKeepAliveMaxRequest(Long maxRequestCount) {
      this.keepAliveMaxRequest = maxRequestCount;
      return this;
    }

    public Long getKeepAliveTimeOut() {
        return keepAliveTimeOut;
    }

    public Backend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        this.keepAliveTimeOut = keepAliveTimeOut;
        return this;
    }

    public boolean isKeepAliveLimit() {
        Long now = System.currentTimeMillis();
        if (requestCount<=keepAliveMaxRequest) {
            requestCount++;
        }
        if ((requestCount>=keepAliveMaxRequest) || (requestCount==Long.MAX_VALUE) ||
                (now-keepAliveTimeMark)>keepAliveTimeOut) {
            keepAliveTimeMark = now;
            requestCount = 0L;
            return true;
        }
        return false;
    }

    public Integer getMaxPoolSize() {
        return backendMaxPoolSize;
    }

    public Backend setMaxPoolSize(Integer maxPoolSize) {
        this.backendMaxPoolSize = maxPoolSize;
        return this;
    }

    public JsonObject zeroConnectionJson() {
        JsonObject myConnections = new JsonObject();
        myConnections.putString(uuidFieldName, myUUID);
        myConnections.putNumber(numConnectionFieldName, 0);
        return myConnections;
    }

    // Lazy initialization
    public HttpClient connect(String remoteIP, String remotePort) {
        final String backend = this.toString();
        if (client==null) {
            if (vertx!=null) {
                client = vertx.createHttpClient()
                    .setKeepAlive(keepalive)
                    .setTCPKeepAlive(keepalive)
                    .setConnectTimeout(connectionTimeout)
                    .setMaxPoolSize(backendMaxPoolSize);
                if (host!=null || port!=null) {
                    client.setHost(host)
                          .setPort(port);
                }
                client.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable e) {
                        eb.publish(QUEUE_HEALTHCHECK_FAIL, backend);
                        eb.publish(queueActiveConnections, zeroConnectionJson());
                    }
                });
                if (!registered) {
                    eb.registerLocalHandler(queueActiveConnections, getHandlerListenGlobalConnections());
                    registered = true;
                }
            }
        }
        addConnection(remoteIP, remotePort);
        return client;
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

    public void close() {
        if (client!=null) {
            try {
                client.close();
            } catch (IllegalStateException e) {
                // Already closed. Ignore exception.
            } finally {
                client=null;
                eb.publish(queueActiveConnections, zeroConnectionJson());
                if (registered) {
                    eb.unregisterHandler(queueActiveConnections, getHandlerListenGlobalConnections());
                    registered = false;
                }
            }
        }
        connections.clear();
    }

    public boolean isClosed() {
        if (client==null) {
            return true;
        }
        boolean httpClientClosed = false;
        try {
            client.getReceiveBufferSize();
        } catch (IllegalStateException e) {
            httpClientClosed = true;
        }
        return httpClientClosed;
    }

    private boolean addConnection(String connectionId) {
        return connections.put(connectionId, System.currentTimeMillis()) == null;
    }

    private boolean addConnection(String host, String port) {
        String connectionId = String.format("%s:%s", host, port);
        return addConnection(connectionId);
    }

    public boolean removeConnection(String connectionId) {
        return connections.remove(connectionId) != null;
    }

    public Integer getActiveConnections() {
        int globalSum = connections.size();
        for (int externalValue: globalConnections.values()) {
            globalSum =+ externalValue;
        }
        return globalSum;
    }

    public Integer getInstanceActiveConnections() {
        return connections.size();
    }

    public boolean isNewConenction(String remoteId) {
        return connections.containsKey(remoteId);
    }

    public boolean isNewConnection(String remoteIP, String remotePort) {
        String remoteId = String.format("%s:%s", remoteIP, remotePort);
        return isNewConenction(remoteId);
    }

    public Long getSchedulerDelay() {
        return schedulerDelay;
    }

    public void setSchedulerDelay(Long schedulerDelay) {
        this.schedulerDelay = schedulerDelay;
        cancelScheduler();
        activeScheduler();
    }

    public void activeScheduler() {
        if (schedulerId==0L && vertx!=null) {
            schedulerId = vertx.setPeriodic(schedulerDelay, new Handler<Long>() {

                @Override
                public void handle(Long event) {
                    Long timeout = System.currentTimeMillis() - schedulerDelay;
                    for (String remote : connections.keySet()) {
                        if (connections.get(remote)<timeout) {
                            removeConnection(remote);
                        }
                    }
                    JsonObject myConnections = new JsonObject();
                    myConnections.putString(uuidFieldName, myUUID);
                    myConnections.putNumber(numConnectionFieldName, connections.size());
                    eb.publish(queueActiveConnections, myConnections);
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