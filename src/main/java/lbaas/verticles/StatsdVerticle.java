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

import static org.vertx.java.core.datagram.InternetProtocolFamily.IPv4;
import static lbaas.metrics.StatsdClient.TypeStatsdMessage;
import static lbaas.core.Constants.CONF_HOST;
import static lbaas.core.Constants.CONF_PORT;
import static lbaas.core.Constants.CONF_PREFIX;

import lbaas.metrics.StatsdClient;

import org.vertx.java.core.Handler;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class StatsdVerticle extends Verticle {

    private StatsdClient statsdClient;
    private String statsDhost;
    private Integer statsDPort;
    private String prefix;

    @Override
    public void start() {

        final Logger log = container.logger();
        final JsonObject conf = container.config();
        this.prefix = conf.getString(CONF_PREFIX, "stats.");
        this.statsDhost = conf.getString(CONF_HOST, "localhost");
        this.statsDPort = conf.getInteger(CONF_PORT, 8125);
        final DatagramSocket dgram = vertx.createDatagramSocket(IPv4).setReuseAddress(true);
        statsdClient = new StatsdClient(statsDhost, statsDPort, prefix, dgram, container.logger());

        final EventBus eb = vertx.eventBus();

        /*
         * Receive from EventBus. Format => tag:num
         */
        eb.registerLocalHandler("statsd.counter", getHandler(TypeStatsdMessage.COUNT));
        eb.registerLocalHandler("statsd.timer", getHandler(TypeStatsdMessage.TIME));
        eb.registerLocalHandler("statsd.gauge", getHandler(TypeStatsdMessage.GAUGE));
        eb.registerLocalHandler("statsd.set", getHandler(TypeStatsdMessage.SET));

        log.info(String.format("Instance %s started", this.toString()));

    }

    private Handler<Message<String>> getHandler(final StatsdClient.TypeStatsdMessage type) {
        return new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                statsdClient.send(type, message.body());
            }
        };
    }
}
