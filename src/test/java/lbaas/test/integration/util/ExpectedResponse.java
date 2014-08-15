package lbaas.test.integration.util;

import org.vertx.java.core.json.JsonObject;

public class ExpectedResponse {
    public int code = 200;
    public JsonObject body = new JsonObject();

    public ExpectedResponse setCode(int code) {
        this.code = code;
        return this;
    }
    public ExpectedResponse setBody(JsonObject body) {
        this.body = body;
        return this;
    }
    

}
