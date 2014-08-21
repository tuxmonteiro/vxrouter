package lbaas.test.integration.util;

import org.vertx.java.core.json.JsonObject;

public class ExpectedResponse {
    private int code = 200;
    private String body;
    private JsonObject bodyJson;
    private int bodySize = -1;


    public int code() {
        return code;
    }
    public String body() {
        return body;
    }
    public JsonObject bodyJson() {
        return bodyJson;
    }
    public int bodySize() {
        return bodySize;
    }

    public ExpectedResponse setCode(int code) {
        this.code = code;
        return this;
    }
    public ExpectedResponse setBody(String body) {
        this.body = body;
        return this;
    }
    public ExpectedResponse setBodyJson(JsonObject body) {
        this.bodyJson = body;
        return this;
    }
    public ExpectedResponse setBodyJson(String body) {
        this.bodyJson = Util.safeExtractJson(body);;
        return this;
    }
    public ExpectedResponse setBodySize(int bytes) {
        this.bodySize = bytes;
        return this;
    }

}
