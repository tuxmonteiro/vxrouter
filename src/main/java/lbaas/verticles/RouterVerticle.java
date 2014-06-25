/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.verticles;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lbaas.Client;
import lbaas.CounterWithStatsd;
import lbaas.ICounter;
import lbaas.QueueMap;
import lbaas.Server;
import lbaas.handlers.RouterRequestHandler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class RouterVerticle extends Verticle {

  @Override
  public void start() {

      final Logger log = container.logger();
      final JsonObject conf = container.config();
      final ICounter counter = new CounterWithStatsd(conf, vertx, log);

      final Map<String, Set<Client>> vhosts = new HashMap<>();
      final Map<String, Set<Client>> badVhosts = new HashMap<>();
      final QueueMap queueMap = new QueueMap(this, vhosts, badVhosts);

      queueMap.registerQueueAdd();
      queueMap.registerQueueDel();

      final Server server = new Server(vertx, container, counter);

      final Handler<HttpServerRequest> handlerHttpServerRequest = 
              new RouterRequestHandler(vertx, container, vhosts, server, counter);

      server.start(this, handlerHttpServerRequest, 9000);
      log.info(String.format("Instance %s started", this.toString()));

   }

}
