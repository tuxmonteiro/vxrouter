package lbaas;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class RouteManagerVerticle extends Verticle {

    @Override
    public void start() {

        final EventBus eventBus = vertx.eventBus();
        final JsonObject conf = container.config();

        Handler<HttpServerRequest> handlerRouterManager = new Handler<HttpServerRequest>() {

            @Override
            public void handle(final HttpServerRequest sRequest) {
                sRequest.bodyHandler(new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer body) {
                        eventBus.publish("router.add", body.toString());
                        sRequest.response().end();
                    }
                });

            }
        };

        vertx.createHttpServer().requestHandler(handlerRouterManager)
        .listen(conf.getInteger("port",9001));
    }

}
