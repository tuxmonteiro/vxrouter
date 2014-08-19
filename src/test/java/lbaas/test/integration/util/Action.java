package lbaas.test.integration.util;

import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class Action {
	private UtilTestVerticle verticle;
	private RequestForTest request = new RequestForTest();;
	private ExpectedResponse response = new ExpectedResponse();;
	private String id = UUID.randomUUID().toString();
    private boolean dontStop;

    public Action(UtilTestVerticle originVerticle) {
    	verticle = originVerticle;
    }
	
	public String id() {
		return id;
	}

	public boolean dontStop() {
		return dontStop;
	}

	public Action setDontStop(boolean dontStop) {
		this.dontStop = dontStop;
		return this;
	}

	public RequestForTest request() {
		return request;
	}

	public ExpectedResponse response() {
		return response;
	}

    public Action setRequest(RequestForTest request) {
        this.request = request;
        return this;
    }
    public Action setResponse(ExpectedResponse response) {
        this.response = response;
        return this;
    }

    public Action after(final Action previous) {
    	previous.dontStop = true;
    	EventBus eb = verticle.getVertx().eventBus();
    	eb.registerHandler("ended.action", new Handler<Message<String>>() {
    		@Override
    		public void handle(Message<String> message) {
    			if (message.body().equals(previous.id)) {
    				run();
    			}
    		};
		});
    	return this;
    }
    
    public void run() {
    	verticle.run(this);
    }

    
    // Request helpers
    public Action usingMethod(String method) {
    	this.request.setMethod(method);
    	return this;
    }
        
    public Action onPort(int port) {
        this.request.setPort(port);
        return this;
    }

    public Action setRequestHost(String host) {
        this.request.setHost(host);
        return this;        
    }

    public Action atUri(String uri) {
        this.request.setUri(uri);
        return this;
    }
    
    public Action setBodyJson(JsonObject body) {
        this.request.setBodyJson(body);
        return this;
    }
    public Action setBodyJson(String body) {
        this.request.setBodyJson(body);
        return this;
    }
    public Action addHeader(String name, String value) {
    	this.request.addHeader(name, value);
    	return this;
    }
    
    // Response helpers
    public Action expectCode(int code) {
        response.setCode(code);
        return this;
    }
    public Action expectJson(JsonObject body) {
        response.setBodyJson(body);
        return this;
    }
    public Action expectJson(String body) {
        response.setBodyJson(body);
        return this;
    }
    
    public Action expectBodySize(int bytes) {
    	response.setBodySize(bytes);
    	return this;
    }
   
}
