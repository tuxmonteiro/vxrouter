/* 
 * Vert.X module StatsD Client implementation
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.verticles;

import static org.vertx.java.core.datagram.InternetProtocolFamily.IPv4;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class StatsDClient extends Verticle {

    private final static String PATTERN_COUNT = "%s:%d|c";
    private final static String PATTERN_TIME  = "%s:%d|ms";
    private final static String PATTERN_GAUGE = "%s:%d|g";
    private final static String PATTERN_SET   = "%s:%d|s";

    public enum TypeStatsdMessage {
        COUNT(PATTERN_COUNT),
        TIME(PATTERN_TIME),
        GAUGE(PATTERN_GAUGE),
        SET(PATTERN_SET);

        private final String pattern;
        private TypeStatsdMessage(String pattern) {
            this.pattern = pattern;
        }
        public String getPattern() {
            return this.pattern;
        }
    }

    private String prefix = "stats";
    private String statsDhost;
    private Integer statsDPort;

    @Override
    public void start() {

        final Logger log = container.logger();
        final JsonObject conf = container.config();
        this.prefix = conf.getString("defaultPrefix", "stats.");
        this.statsDhost = conf.getString("host", "localhost");
        this.statsDPort = conf.getInteger("port", 8125);

        final EventBus eb = vertx.eventBus();

        /*
         * Receive from EventBus. Format => tag:num
         */
        eb.registerLocalHandler("statsd.counter", getHandler(PATTERN_COUNT));
        eb.registerLocalHandler("statsd.timer", getHandler(PATTERN_TIME));
        eb.registerLocalHandler("statsd.gauge", getHandler(PATTERN_GAUGE));
        eb.registerLocalHandler("statsd.set", getHandler(PATTERN_SET));

        log.info(String.format("Instance %s started", this.toString()));

    }

    public StatsDClient(String host, Integer port) {
        this.statsDhost = host;
        this.statsDPort = port;
    }

    public StatsDClient() {
        this("localhost", 8125);
    }

    private Handler<Message<String>> getHandler(final String pattern) {
        final String prefix = this.prefix;
        final Vertx vertx = this.getVertx();

        return new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String[] data = message.body().split(":");
                DatagramSocket socket = vertx.createDatagramSocket(IPv4);
                String id = String.format("".equals(prefix) ? "%s%s": "%s.%s", prefix, data[0]);
                socket.send(String.format(pattern, id, data[1]), statsDhost, statsDPort, null);
            }
        };
    }

    public void sendStatsd(TypeStatsdMessage typeStatsdMessage, String message) {
        String[] data = message.split(":");
        DatagramSocket socket = vertx.createDatagramSocket(IPv4);
        String id = String.format("".equals(prefix) ? "%s%s": "%s.%s", prefix, data[0]);
        socket.send(String.format(typeStatsdMessage.getPattern(), id, data[1]), statsDhost, statsDPort, null);
    }

}
