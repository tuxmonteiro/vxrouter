/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

public class Client {

    private final Vertx vertx;
    private HttpClient client;
    private EventBus eb;

    private final String host;
    private final Integer port;
    private Integer connectionTimeout;
    private boolean keepalive;
    private Long keepAliveMaxRequest;
    private Long keepAliveTimeOut;

    private Long keepAliveTimeMark;
    private Long requestCount;

    private Long schedulerId = 0L;
    private Long schedulerDelay = 10000L;

    private Map<String, Long> connections = new HashMap<>();

    @Override
    public String toString() {
        return String.format("%s:%d", this.host, this.port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Client other = (Client) obj;
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

    public Client(final String hostWithPort, final Vertx vertx) {
        String[] hostWithPortArray = hostWithPort!=null ? hostWithPort.split(":") : null;
        this.vertx = vertx;
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

    public Client setConnectionTimeout(Integer timeout) {
        this.connectionTimeout = timeout;
        return this;
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    public Client setKeepAlive(boolean keepalive) {
        this.keepalive = keepalive;
        return this;
    }

    public Long getKeepAliveMaxRequest() {
      return keepAliveMaxRequest;
    }

    public Client setKeepAliveMaxRequest(Long maxRequestCount) {
      this.keepAliveMaxRequest = maxRequestCount;
      return this;
    }

    public Long getKeepAliveTimeOut() {
        return keepAliveTimeOut;
    }

    public Client setKeepAliveTimeOut(Long keepAliveTimeOut) {
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
        int maxPoolSize = 0;
        if (client!=null) {
            maxPoolSize = client.getMaxPoolSize();
        }
        return maxPoolSize;
    }

    public Client setMaxPoolSize(Integer maxPoolSize) {
        if (client!=null) {
            client.setMaxPoolSize(maxPoolSize);
        }
        return this;
    }

    // Lazy initialization
    public HttpClient connect() {
        final String endpoint = this.toString();
        if (client==null) {
            if (vertx!=null) {
                client = vertx.createHttpClient()
                    .setKeepAlive(keepalive)
                    .setTCPKeepAlive(keepalive)
                    .setConnectTimeout(connectionTimeout);
                if (host!=null || port!=null) {
                    client.setHost(host)
                          .setPort(port);
                }
                client.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable e) {
                        vertx.eventBus().publish(QUEUE_HEALTHCHECK_FAIL, endpoint);
                    }
                });
            } else {
                throw new RuntimeException(String.format("FAIL: Connect impossible (%s). vertx is null", this.toString()));
            }
        }
        return client;
    }

    public void close() {
        if (client!=null) {
            try {
                client.close();
            } catch (IllegalStateException e) {
                // Already closed. Ignore exception.
            } finally {
                client=null;
                connections.clear();
            }
        }
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

    public boolean addConnection(String connectionId) {
        return connections.put(connectionId, System.currentTimeMillis()) == null;
    }

    public boolean addConnection(String host, String port) {
        String connectionId = String.format("%s:%s", host, port);
        return addConnection(connectionId);
    }

    public boolean removeConnection(String connectionId) {
        return connections.remove(connectionId) != null;
    }

    public int getActiveConnections() {
        return connections.size();
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