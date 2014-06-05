/* 
 * Vert.X module StatsD Client implementation
 */
package lbaas.verticles;

import static org.vertx.java.core.datagram.InternetProtocolFamily.IPv4;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class StatsDVerticle extends Verticle {

    private final String PATTERN_COUNT = "%s.%s:%d|c";
    private final String PATTERN_TIME  = "%s.%s:%d|ms";
    private final String PATTERN_GAUGE = "%s.%s:%d|g";
    private final String PATTERN_SET   = "%s.%s:%d|s";

    private String prefix = "stats";
    private String statsDhost;
    private Integer statsDPort;

    public void start() {

        final JsonObject conf = container.config();
        this.prefix = conf.getString("defaultPrefix", "stats");
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

    }

    private Handler<Message<String>> getHandler(final String pattern) {
        final String prefix = this.prefix;
        final Vertx vertx = this.getVertx();

        return new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String[] data = message.body().split(":");
                DatagramSocket socket = vertx.createDatagramSocket(IPv4);
                socket.send(String.format(pattern, prefix, data[0], data[1]), statsDhost, statsDPort, null);
            }
        };
    }

}
