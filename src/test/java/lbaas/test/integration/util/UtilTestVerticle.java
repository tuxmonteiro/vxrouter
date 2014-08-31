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
package lbaas.test.integration.util;

import static lbaas.core.Constants.CONF_INSTANCES;
import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.assertNotNull;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public abstract class UtilTestVerticle extends TestVerticle {

    private boolean routerStarted;
    private boolean routeManagerStarted;

    public void testCompleteWrapper() {
        routerStarted = routeManagerStarted = false;
        testComplete();
    }

    @Override
    public void start() {

        EventBus eb = vertx.eventBus();
        eb.registerLocalHandler("init.server", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> event) {
                JsonObject messageJson = Util.safeExtractJson(event.body());
                if (messageJson.getString("id").startsWith("lbaas.verticles.RouterVerticle"))
                    routerStarted = true;
                if (messageJson.getString("id").startsWith("lbaas.verticles.RouteManagerVerticle"))
                    routeManagerStarted = true;
                if (routerStarted && routeManagerStarted) {
                    startTests();
                }
            }}
                );

        initialize();
        JsonObject config = new JsonObject().putObject("router",
                new JsonObject().putNumber(CONF_INSTANCES, 1).putNumber("backendRequestTimeOut", 1000));
        container.deployModule(System.getProperty("vertx.modulename"), config, new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                assertTrue(asyncResult.succeeded());
                assertNotNull("deploymentID should not be null", asyncResult.result());
            }
        });
    }
    

    public RequestForTest newRequest() {
        return new RequestForTest();
    }
    public ExpectedResponse newResponse() {
        return new ExpectedResponse();
    }
    public Action newAction() {
    	return new Action(this);
    }
    public Action newGet() {
    	return new Action(this).usingMethod("GET");
    }
    public Action newPost() {
    	return new Action(this).usingMethod("POST");
    }
    

    public void run(final Action action) {
        final RequestForTest req = action.request();
        final ExpectedResponse exp = action.response();
        HttpClient client = vertx.createHttpClient().setPort(req.port()).setHost(req.host());

        HttpClientRequest clientRequest = client.request(req.method(), req.uri(), new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse resp) {
                assertEquals(exp.code(), resp.statusCode());

                final Buffer body = new Buffer(0);

                resp.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer data) {
                        body.appendBuffer(data);
                    }
                });

                resp.endHandler(new Handler<Void>() {
                    public void handle(Void v) {
                        // Assert body as String
                        if (exp.body() != null) {
                            assertEquals(exp.body(), body.toString());
                        }
                        // Assert body as Json
                        if (exp.bodyJson() != null) {
                            JsonObject respJson = Util.safeExtractJson(body.toString());
                            assertEquals(exp.bodyJson(), respJson);
                        }
                        // Assert body size
                        if (exp.bodySize() != -1) {
                            assertEquals(exp.bodySize(), body.length());
                        }
                        // Complete test or go on with the next action
                        if (action.dontStop()) {
                            EventBus eb = vertx.eventBus();
                            eb.publish("ended.action", action.id());
                        } else {
                            testCompleteWrapper();
                        }
                    }
                });
            }
        });
        clientRequest.headers().set(req.headers());
        clientRequest.setChunked(true); // To avoid calculating content length
        if (req.bodyJson() != null) {
            clientRequest.write(req.bodyJson().toString());
        }
        clientRequest.end();

    }

}
