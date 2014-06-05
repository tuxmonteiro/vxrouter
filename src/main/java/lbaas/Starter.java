/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.CONF_INSTANCES;

public class Starter extends Verticle{

    @Override
    public void start() {
        final JsonObject conf = container.config();
        final JsonObject confRouter = conf.getObject("router", new JsonObject("{}"));
        final JsonObject confRouterManager = conf.getObject("routermanager", new JsonObject("{}"));
        final JsonObject confHealthManager = conf.getObject("healthmanager", new JsonObject("{}"));
        final JsonObject confStatsd = conf.getObject("statsd", new JsonObject("{}"));

        int numCpuCores = Runtime.getRuntime().availableProcessors();
        container.deployVerticle("lbaas.verticles.RouterVerticle", confRouter, confRouter.getInteger(CONF_INSTANCES, numCpuCores));
        container.deployVerticle("lbaas.verticles.RouteManagerVerticle", confRouterManager, confRouterManager.getInteger(CONF_INSTANCES, 1));
        container.deployVerticle("lbaas.verticles.HealthManagerVerticle", confHealthManager, confHealthManager.getInteger(CONF_INSTANCES, 1));
        container.deployVerticle("lbaas.verticles.StatsDVerticle", confStatsd, confStatsd.getInteger(CONF_INSTANCES, 1));

    }
}
