package lbaas;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class Starter extends Verticle{

    @Override
    public void start() {
        final JsonObject conf = container.config();
        final JsonObject conf_router = conf.getObject("router", new JsonObject("{}"));
        final JsonObject conf_routermanager = conf.getObject("routermanager", new JsonObject("{}"));

        int num_cpu_cores = Runtime.getRuntime().availableProcessors();
        container.deployVerticle("lbaas.RouterVerticle", conf_router, conf_router.getInteger("instances", num_cpu_cores));
        container.deployVerticle("lbaas.RouteManagerVerticle", conf_routermanager, conf_routermanager.getInteger("instances", 1));
    }
}
