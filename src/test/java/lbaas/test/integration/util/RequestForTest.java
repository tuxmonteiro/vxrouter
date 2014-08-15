package lbaas.test.integration.util;

import org.vertx.java.core.MultiMap;

public class RequestForTest {
    public int port = 0;
    public String host = "localhost";
    public String uri = "/";
    public MultiMap headers = null;


    public RequestForTest setPort(int port) {
        this.port = port;
        return this;
    }

    public RequestForTest setHost(String host) {
        this.host = host;
        return this;        
    }

    public RequestForTest setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public RequestForTest setHeaders(MultiMap headers) {
        this.headers = headers;
        return this;
    }
    
}
