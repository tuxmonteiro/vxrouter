/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.logging.Logger;

public class StatsdClient {
    private final static String PATTERN_COUNT = "%s:%s|c";
    private final static String PATTERN_TIME  = "%s:%s|ms";
    private final static String PATTERN_GAUGE = "%s:%s|g";
    private final static String PATTERN_SET   = "%s:%s|s";

    public static enum TypeStatsdMessage {
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

    private String statsDhost;
    private Integer statsDPort;
    private String prefix;
    private final Logger log;
    private final DatagramSocket socket;

    public StatsdClient(String statsDhost, Integer statsDPort, String prefix,
                        final DatagramSocket socket, final Logger log) {
        this.statsDhost = statsDhost;
        this.statsDPort = statsDPort;
        this.prefix = "".equals(prefix) ? "stats" : prefix;
        this.log = log;
        this.socket = socket;
    }

    public StatsdClient(final DatagramSocket socket, final Logger log) {
        this("localhost", 8125, "", socket, log);
    }

    public void send(final TypeStatsdMessage type, String message) {
        String[] data = message.split(":");
        String key = data[0];
        String value = data[1];
        try {
            String id = String.format("".equals(prefix) ? "%s%s": "%s.%s", prefix, key);
            socket.send(String.format(type.getPattern(), id, value), statsDhost, statsDPort, null);
        } catch (io.netty.channel.ChannelException e) {
            log.error("io.netty.channel.ChannelException: Failed to open a socket.");
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
    }
}
