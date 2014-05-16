package lbaas;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;

public class Client {

    private final Vertx vertx;
    private HttpClient client;
    private String host;
    private Integer port;
    private Integer timeout;
    private Integer maxPoolSize;

    private boolean keepalive;
    private Long keepAliveMaxRequest;
    private Long keepAliveTimeMark;
    private Long keepAliveTimeOut;
    private Long requestCount;

    public Client(final String hostWithPort, final Vertx vertx) {
        this.vertx = vertx;
        this.client = null;
        this.host = hostWithPort.split(":")[0];
        this.port = Integer.parseInt(hostWithPort.split(":")[1]);
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

    public Long getKeepAliveRequestCount() {
      return requestCount;
    }

    public Long getMaxRequestCount() {
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
        if ((requestCount>=keepAliveMaxRequest) || ((now-keepAliveTimeMark))>keepAliveTimeOut) {
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
        if (client==null) {
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
//                  System.err.println(e.getMessage());
              }
            });
        }
        return client;
    }

    public void close() {
        if (client!=null) {
            try {
                client.close();
            } catch (IllegalStateException e) {
                // Already closed
//                System.err.println(e.getMessage());
            } finally {
                client=null;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==null||!(obj instanceof Client)) {
            return false;
        }
        Client objClient = (Client)obj;
        return objClient.getHost() == this.host && objClient.getPort() == this.port;
    }

    @Override
    public int hashCode() {
        return String.format("%s:%d", this.host, this.port).hashCode();
    }
}