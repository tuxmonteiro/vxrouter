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

import static lbaas.core.Constants.*;


public class Starter extends Verticle{

    @Override
    public void start() {
        final JsonObject conf = container.config();
        final JsonObject confRouter = conf.getObject(CONF_ROOT_ROUTER, new JsonObject("{}"));
        final JsonObject confRouterManager = conf.getObject(CONF_ROOT_ROUTEMANAGER, new JsonObject("{}"));
        final JsonObject confHealthManager = conf.getObject(CONF_ROOT_HEALTHMANAGER, new JsonObject("{}"));
        final JsonObject confStatsd;
        if (conf.containsField(CONF_ROOT_STATSD)) {
            confStatsd = conf.getObject(CONF_ROOT_STATSD, new JsonObject("{}"));
            container.deployVerticle(StatsdVerticle.class.getName(), confStatsd, confStatsd.getInteger(CONF_INSTANCES, 1));
            confRouter.putBoolean(CONF_STATSD_ENABLE, true);
            confRouter.putString(CONF_STATSD_HOST, confStatsd.getString(CONF_HOST, "localhost"));
            confRouter.putString(CONF_STATSD_PREFIX, confStatsd.getString(CONF_PREFIX, "stats"));
            confRouter.putNumber(CONF_STATSD_PORT, confStatsd.getInteger(CONF_PORT, 8125));

            confRouterManager.putBoolean(CONF_STATSD_ENABLE, true);
            confRouterManager.putString(CONF_STATSD_HOST, confStatsd.getString(CONF_HOST, "localhost"));
            confRouterManager.putString(CONF_STATSD_PREFIX, confStatsd.getString(CONF_PREFIX, "stats"));
            confRouterManager.putNumber(CONF_STATSD_PORT, confStatsd.getInteger(CONF_PORT, 8125));
        }

        int numCpuCores = Runtime.getRuntime().availableProcessors();
        container.deployVerticle(RouterVerticle.class.getName(), confRouter, confRouter.getInteger(CONF_INSTANCES, numCpuCores));
        container.deployVerticle(RouteManagerVerticle.class.getName(), confRouterManager, confRouterManager.getInteger(CONF_INSTANCES, 1));
        container.deployVerticle(HealthManagerVerticle.class.getName(), confHealthManager, confHealthManager.getInteger(CONF_INSTANCES, 1));
    }
}
