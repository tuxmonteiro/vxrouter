package lbaas.test.integration.util;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.json.JsonObject;

public class RequestForTest {
    private int port = 80;
    private String host = "localhost";
    private String uri = "/";
    private MultiMap headers = null;
    private String method = "GET";
    private JsonObject bodyJson;

    public int port() {
		return port;
	}
	public String host() {
		return host;
	}
	public String uri() {
		return uri;
	}
	public String method() {
		return method;
	}
	public JsonObject bodyJson() {
		return bodyJson;
	}
	public MultiMap headers() {
    	if (headers == null)
    		headers = new CaseInsensitiveMultiMap();
    	return headers;
    }
    public RequestForTest addHeader(String name, String value) {
    	headers().add(name, value);
    	return this;
    }
    public RequestForTest setBodyJson(JsonObject bodyJson) {
        this.bodyJson = bodyJson;
        return this;
    }
    public RequestForTest setBodyJson(String bodyJson) {
        this.bodyJson = Util.safeExtractJson(bodyJson);;
        return this;
    }
    
    public RequestForTest setMethod(String method) {
		this.method = method;
		return this;
	}

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
