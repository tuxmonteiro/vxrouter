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

    private String prefix = "stats";
    private String statsDhost;
    private Integer statsDPort;

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
        eb.registerLocalHandler("statsd.counter", getHandler(TypeStatsdMessage.COUNT));
        eb.registerLocalHandler("statsd.timer", getHandler(TypeStatsdMessage.TIME));
        eb.registerLocalHandler("statsd.gauge", getHandler(TypeStatsdMessage.GAUGE));
        eb.registerLocalHandler("statsd.set", getHandler(TypeStatsdMessage.SET));

        log.info(String.format("Instance %s started", this.toString()));

    }

    public StatsDClient(String statsDhost, Integer statsDPort) {
        this.statsDhost = statsDhost;
        this.statsDPort = statsDPort;
    }

    public StatsDClient() {
        this("localhost", 8125);
    }

    private Handler<Message<String>> getHandler(final TypeStatsdMessage type) {
        return new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                sendStatsd(type, message.body());
            }
        };
    }

    public void sendStatsd(final TypeStatsdMessage type, String message) {
        if (vertx!=null) {
            sendStatsd(type, message, vertx, container.logger());
        }
    }

    public void sendStatsd(final TypeStatsdMessage type, String message, final Vertx vertx, final Logger log) {
        String[] data = message.split(":");
        Long num = Long.parseLong(data[1]);
        DatagramSocket socket = null;
        try {
            socket = vertx.createDatagramSocket(IPv4);
            String id = String.format("".equals(prefix) ? "%s%s": "%s.%s", prefix, data[0]);
            socket.send(String.format(type.getPattern(), id, num), statsDhost, statsDPort, null);
        } catch (io.netty.channel.ChannelException e) {
            log.error("io.netty.channel.ChannelException: Failed to open a socket.");
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        } finally {
            if (socket!=null) {
                socket.close();
            }
        }
    }

}
