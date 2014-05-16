package lbaas;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class Starter extends Verticle{

    @Override
    public void start() {
        final JsonObject conf = container.config();
        final JsonObject conf_router = conf.getObject("router", new JsonObject("{}"));
        final JsonObject conf_routermanager = conf.getObject("routermanager", new JsonObject("{}"));

        container.deployVerticle("lbaas.RouterVerticle", conf_router, conf_router.getInteger("instances", 4));
        container.deployVerticle("lbaas.RouterManagerVerticle", conf_routermanager, conf_routermanager.getInteger("instances", 1));
    }
}
