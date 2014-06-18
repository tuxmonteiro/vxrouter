package lbaas;

import static org.vertx.java.core.datagram.InternetProtocolFamily.IPv4;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.logging.Logger;

public class StatsdClient {
    private final static String PATTERN_COUNT = "%s:%d|c";
    private final static String PATTERN_TIME  = "%s:%d|ms";
    private final static String PATTERN_GAUGE = "%s:%d|g";
    private final static String PATTERN_SET   = "%s:%d|s";

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
                        final Vertx vertx, final Logger log) {
        this.statsDhost = statsDhost;
        this.statsDPort = statsDPort;
        this.prefix = "".equals(prefix) ? "stats" : prefix;
        this.log = log;
        this.socket = vertx.createDatagramSocket(IPv4).setReuseAddress(true);
    }

    public StatsdClient(final Vertx vertx, final Logger log) {
        this("localhost", 8125, "", vertx, log);
    }

    public void sendStatsd(final TypeStatsdMessage type, String message) {
        String[] data = message.split(":");
        Long num = Long.parseLong(data[1]);
        try {
            String id = String.format("".equals(prefix) ? "%s%s": "%s.%s", prefix, data[0]);
            socket.send(String.format(type.getPattern(), id, num), statsDhost, statsDPort, null);
        } catch (io.netty.channel.ChannelException e) {
            log.error("io.netty.channel.ChannelException: Failed to open a socket.");
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
//        finally {
//            if (socket!=null) {
//                socket.close();
//            }
//        }
    }
}
