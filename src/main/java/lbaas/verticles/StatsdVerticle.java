/* 
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.verticles;

import static lbaas.StatsdClient.TypeStatsdMessage;
import lbaas.StatsdClient;

import org.vertx.java.core.Handler;
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

    public void start() {

        final Logger log = container.logger();
        final JsonObject conf = container.config();
        this.prefix = conf.getString("defaultPrefix", "stats.");
        this.statsDhost = conf.getString("host", "localhost");
        this.statsDPort = conf.getInteger("port", 8125);
        statsdClient = new StatsdClient(statsDhost, statsDPort, prefix, vertx, container.logger());

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
                statsdClient.sendStatsd(type, message.body());
            }
        };
    }
}
