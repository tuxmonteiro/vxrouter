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

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import static lbaas.core.Constants.CONF_INSTANCES;

public class Starter extends Verticle{

    @Override
    public void start() {
        final JsonObject conf = container.config();
        final JsonObject confRouter = conf.getObject("router", new JsonObject("{}"));
        final JsonObject confRouterManager = conf.getObject("routermanager", new JsonObject("{}"));
        final JsonObject confHealthManager = conf.getObject("healthmanager", new JsonObject("{}"));
        final JsonObject confStatsd;
        if (conf.containsField("statsd")) {
            confStatsd = conf.getObject("statsd", new JsonObject("{}"));
            container.deployVerticle("lbaas.verticles.StatsdVerticle", confStatsd, confStatsd.getInteger(CONF_INSTANCES, 1));
            confRouter.putBoolean("enableStatsd", true);
            confRouter.putString("statsdHost", confStatsd.getString("host", "localhost"));
            confRouter.putString("statsdPrefix", confStatsd.getString("prefix", "stats"));
            confRouter.putNumber("statsdPort", confStatsd.getInteger("port", 8125));

            confRouterManager.putBoolean("enableStatsd", true);
            confRouterManager.putString("statsdHost", confStatsd.getString("host", "localhost"));
            confRouterManager.putString("statsdPrefix", confStatsd.getString("prefix", "stats"));
            confRouterManager.putNumber("statsdPort", confStatsd.getInteger("port", 8125));
        }

        int numCpuCores = Runtime.getRuntime().availableProcessors();
        container.deployVerticle("lbaas.verticles.RouterVerticle", confRouter, confRouter.getInteger(CONF_INSTANCES, numCpuCores));
        container.deployVerticle("lbaas.verticles.RouteManagerVerticle", confRouterManager, confRouterManager.getInteger(CONF_INSTANCES, 1));
        container.deployVerticle("lbaas.verticles.HealthManagerVerticle", confHealthManager, confHealthManager.getInteger(CONF_INSTANCES, 1));
    }
}
