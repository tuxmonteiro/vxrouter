package lbaas.test.integration.util;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.testComplete;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public abstract class UtilTestVerticle extends TestVerticle {

    public JsonObject safeExtractJson(String s) {
        JsonObject json = null;
        try {
            json = new JsonObject(s);
        } catch (DecodeException e) {
            System.out.println("The string is not a Json. Test will fail");
        }
        return json;
    }

    public void getAndTest(int port, String uri, final int expectedCode, final JsonObject expectedJson) {
        vertx.createHttpClient().setPort(port).getNow(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                assertEquals(expectedCode, resp.statusCode());

                resp.bodyHandler(new Handler<Buffer>() {
                    public void handle(Buffer body) {
                        JsonObject respJson = safeExtractJson(body.toString());
                        assertEquals(expectedJson, respJson);
                        testComplete();
                    }
                });
            }
        });
    }

    public void getAndTest(JsonObject parameters) {
        int port = parameters.getInteger("port");
        String uri = parameters.getString("uri");
        int expectedCode = parameters.getInteger("expectedCode");
        JsonObject expectedJson = parameters.getObject("expectedJson");

        getAndTest(port, uri, expectedCode, expectedJson);
    }

    public void postAndTest(int port, String uri, JsonObject bodyJson, final int expectedCode, final JsonObject expectedJson) {
        HttpClient client = vertx.createHttpClient().setPort(port).setHost("localhost");
        HttpClientRequest request = client.post(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                assertEquals(expectedCode, resp.statusCode());

                resp.bodyHandler(new Handler<Buffer>() {
                    public void handle(Buffer body) {
                        JsonObject respJson = safeExtractJson(body.toString());
                        assertEquals(expectedJson, respJson);
                    }
                });
                resp.endHandler(new Handler<Void>() {
                    public void handle(Void v) {
                        testComplete();
                    }
                });
            }
        });
        request.setChunked(true); // To avoid calculating content length

        request.write(bodyJson.toString());
        request.end();
    }
    
    public void postAndTest(JsonObject parameters) {
        int port = parameters.getInteger("port");
        String uri = parameters.getString("uri");
        JsonObject bodyJson = parameters.getObject("bodyJson");
        int expectedCode = parameters.getInteger("expectedCode");
        JsonObject expectedJson = parameters.getObject("expectedJson");

        postAndTest(port, uri, bodyJson, expectedCode, expectedJson);
    }

    public void callMethod(final String nextMethodString, final JsonObject parameters) {
        Method nextMethod = null;
        try {
            nextMethod = UtilTestVerticle.class.getMethod(nextMethodString, new Class<?>[] { JsonObject.class });
        } catch (NoSuchMethodException | SecurityException e1) {
            System.out.println("Method not found");
            e1.printStackTrace();
            return;
        }
        try {
            nextMethod.invoke(this, parameters);
        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            System.out.println("Method execution failed");
            e.printStackTrace();
        }
    }

    public void getAndTestMore(int port, String uri, final int expectedCode, final JsonObject expectedJson,
            final String nextMethodString, final JsonObject parameters) {
        vertx.createHttpClient().setPort(port).getNow(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                assertEquals(expectedCode, resp.statusCode());

                resp.bodyHandler(new Handler<Buffer>() {
                    public void handle(Buffer body) {
                        JsonObject respJson = safeExtractJson(body.toString());
                        assertEquals(expectedJson, respJson);
                    }
                });
                resp.endHandler(new Handler<Void>() {
                    public void handle(Void v) {
                        callMethod(nextMethodString, parameters);
                    }
                });
            }
        });
    }

    public void getAndTestMore(JsonObject parameters) {
        int port = parameters.getInteger("port");
        String uri = parameters.getString("uri");
        int expectedCode = parameters.getInteger("expectedCode");
        JsonObject expectedJson = parameters.getObject("expectedJson");
        String nextMethodString = parameters.getString("nextMethodString");
        JsonObject nextParameters = parameters.getObject("nextParameters");

        getAndTestMore(port, uri, expectedCode, expectedJson, nextMethodString, nextParameters);
    }

    public void postAndTestMore(int port, String uri, JsonObject bodyJson, final int expectedCode, final JsonObject expectedJson,
            final String nextMethodString, final JsonObject parameters) {

        HttpClient client = vertx.createHttpClient().setPort(port).setHost("localhost");
        HttpClientRequest request = client.post(uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                assertEquals(expectedCode, resp.statusCode());

                resp.bodyHandler(new Handler<Buffer>() {
                    public void handle(Buffer body) {
                        JsonObject respJson = safeExtractJson(body.toString());
                        assertEquals(expectedJson, respJson);
                    }
                });
                resp.endHandler(new Handler<Void>() {
                    public void handle(Void v) {
                        callMethod(nextMethodString, parameters);
                    }
                });
            }
        });
        request.setChunked(true); // To avoid calculating content length

        request.write(bodyJson.toString());
        request.end();
    }

    public void postAndTestMore(JsonObject parameters) {
        int port = parameters.getInteger("port");
        String uri = parameters.getString("uri");
        JsonObject bodyJson = parameters.getObject("bodyJson");
        int expectedCode = parameters.getInteger("expectedCode");
        JsonObject expectedJson = parameters.getObject("expectedJson");
        String nextMethodString = parameters.getString("nextMethodString");
        JsonObject nextParameters = parameters.getObject("nextParameters");

        postAndTestMore(port, uri, bodyJson, expectedCode, expectedJson, nextMethodString, nextParameters);
    }
}
