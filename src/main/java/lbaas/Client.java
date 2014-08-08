/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;

import static lbaas.Constants.QUEUE_HEALTHCHECK_FAIL;

public class Client {

    private final Vertx vertx;
    private HttpClient client;

    private String host;
    private Integer port;
    private Integer timeout;
    private Integer maxPoolSize;
    private boolean keepalive;
    private Long keepAliveMaxRequest;
    private Long keepAliveTimeOut;

    private Long keepAliveTimeMark;
    private Long requestCount;

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
        String[] hostWithPortArray = hostWithPort.split(":");
        this.vertx = vertx;
        this.client = null;
        this.host = hostWithPortArray[0];
        this.port = Integer.parseInt(hostWithPortArray[1]);
        this.timeout = 60000;
        this.keepalive = true;
        this.keepAliveMaxRequest = Long.MAX_VALUE-1;
        this.keepAliveTimeMark = System.currentTimeMillis();
        this.keepAliveTimeOut = 86400000L; // One day
        this.requestCount = 0L;
    }

    public String getHost() {
        return host;
    }

    public Client setHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public Client setPort(Integer port) {
        this.port = port;
        return this;
    }

    public Integer getConnectionTimeout() {
        return timeout;
    }

    public Client setConnectionTimeout(Integer timeout) {
        this.timeout = timeout;
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
        if ((requestCount>=keepAliveMaxRequest) || (now-keepAliveTimeMark)>keepAliveTimeOut) {
            keepAliveTimeMark = now;
            requestCount = 0L;
            return true;
        }
        return false;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public Client setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
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
                    .setConnectTimeout(timeout)
                    .setHost(host)
                    .setPort(port)
                    .setMaxPoolSize(maxPoolSize);
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
            }
        }
    }

}