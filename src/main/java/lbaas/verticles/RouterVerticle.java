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
package lbaas.verticles;

import java.util.HashMap;
import java.util.Map;

import lbaas.CounterWithStatsd;
import lbaas.ICounter;
import lbaas.QueueMap;
import lbaas.Server;
import lbaas.Virtualhost;
import lbaas.handlers.RouterRequestHandler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class RouterVerticle extends Verticle {

    public static Vertx sharedVertx;

  @Override
  public void start() {

      sharedVertx = vertx;
      final Logger log = container.logger();
      final JsonObject conf = container.config();
      final ICounter counter = new CounterWithStatsd(conf, vertx, log);

      final Map<String, Virtualhost> virtualhosts = new HashMap<>();
      final QueueMap queueMap = new QueueMap(this, virtualhosts);

      queueMap.registerQueueAdd();
      queueMap.registerQueueDel();

      final Server server = new Server(vertx, container, counter);

      final Handler<HttpServerRequest> handlerHttpServerRequest =
              new RouterRequestHandler(vertx, container, virtualhosts, server, counter);

      server.start(this, handlerHttpServerRequest, 9000);
      log.info(String.format("Instance %s started", this.toString()));

   }

}
