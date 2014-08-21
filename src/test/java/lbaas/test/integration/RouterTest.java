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
package lbaas.test.integration;

import lbaas.test.integration.util.Action;
import lbaas.test.integration.util.UtilTestVerticle;

import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;


public class RouterTest extends UtilTestVerticle {
    @Test
    public void testRouterWhenEmpty() {
        newGet().onPort(9000).addHeader("Host", "www.unknownhost1.com").expectCode(400).expectBodySize(0).run();   	
    }

    @Test
    public void testRouterWith1VHostAndNoBackend() {
        JsonObject vhostJson = new JsonObject().putString("name", "test.localdomain");
        JsonObject expectedJson = new JsonObject().putString("status_message", "OK");

        Action action1 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/virtualhost").expectBodyJson(expectedJson);

        newGet().onPort(9000).addHeader("Host", "test.localdomain").expectCode(400).expectBodySize(0).after(action1);
        action1.run();

    }

    @Test
    public void testRouterNoVHostAddBackend() {
        JsonObject backend = new JsonObject().putString("host", "1.2.3.4").putNumber("port", 80);

        JsonObject vhostJson = new JsonObject().putString("name", "test.localdomain")
                .putArray("backends", new JsonArray().addObject(backend));

        JsonObject expectedJson = new JsonObject().putString("status_message", "OK");

        Action action1 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/backend").expectBodyJson(expectedJson);

        newGet().onPort(9000).addHeader("Host", "test.localdomain").expectCode(400).expectBodySize(0).after(action1);
        action1.run();

    }

    @Test
    public void testRouterWith1VHostAnd1ClosedBackend() {
        JsonObject backend = new JsonObject().putString("host", "127.0.0.1").putNumber("port", 8888);

        JsonObject vhostJson = new JsonObject().putString("name", "test.localdomain")
                .putArray("backends", new JsonArray().addObject(backend));

        JsonObject expectedJson = new JsonObject().putString("status_message", "OK");

        Action action1 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/virtualhost").expectBodyJson(expectedJson);

        Action action2 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/backend").expectBodyJson(expectedJson).after(action1);

        newGet().onPort(9000).addHeader("Host", "test.localdomain").expectCode(502).expectBodySize(0).after(action2);

        action1.run();

    }
    
    @Test
    public void testRouterWith1VHostAnd1TimeoutBackend() {
        // The timeout is set to 1s at test initialization
        JsonObject backend = new JsonObject().putString("host", "1.2.3.4").putNumber("port", 8888);

        JsonObject vhostJson = new JsonObject().putString("name", "test.localdomain")
                .putArray("backends", new JsonArray().addObject(backend));

        JsonObject expectedJson = new JsonObject().putString("status_message", "OK");

        Action action1 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/virtualhost").expectBodyJson(expectedJson);

        Action action2 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/backend").expectBodyJson(expectedJson).after(action1);

        newGet().onPort(9000).addHeader("Host", "test.localdomain").expectCode(504).expectBodySize(0).after(action2);

        action1.run();

    }
    
    @Test
    public void testRouterWith1VHostAnd1RunningBackend() {
        // Create backend
        final HttpServer server = vertx.createHttpServer();
        server.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest request) {
                request.response().setChunked(true).write("response from backend").end();
            }
        });
        server.listen(8888, "localhost");

        // Create Jsons
        JsonObject backend = new JsonObject().putString("host", "127.0.0.1").putNumber("port", 8888);
        JsonObject vhostJson = new JsonObject().putString("name", "test.localdomain")
                .putArray("backends", new JsonArray().addObject(backend));
        JsonObject expectedJson = new JsonObject().putString("status_message", "OK");

        // Create Actions
        Action action1 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/virtualhost").expectBodyJson(expectedJson);
        Action action2 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/backend").expectBodyJson(expectedJson).after(action1);
        final Action action3 = newGet().onPort(9000).addHeader("Host", "test.localdomain")
                .expectCode(200).expectBody("response from backend").after(action2).setDontStop();

        // Create handler to close server after the test
        getVertx().eventBus().registerHandler("ended.action", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                if (message.body().equals(action3.id())) {
                    server.close();
                    testCompleteWrapper();
                }
            };
        });
        
        action1.run();
    }

}
