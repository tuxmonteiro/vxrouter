package lbaas;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import static lbaas.Constants.CONF_INSTANCES;

public class Starter extends Verticle{

    @Override
    public void start() {
        final JsonObject conf = container.config();
        final JsonObject confRouter = conf.getObject("router", new JsonObject("{}"));
        final JsonObject confRouterManager = conf.getObject("routermanager", new JsonObject("{}"));
        final JsonObject confHealthManager = conf.getObject("healthmanager", new JsonObject("{}"));

        int numCpuCores = Runtime.getRuntime().availableProcessors();
        container.deployVerticle("lbaas.RouterVerticle", confRouter, confRouter.getInteger(CONF_INSTANCES, numCpuCores));
        container.deployVerticle("lbaas.RouteManagerVerticle", confRouterManager, confRouterManager.getInteger(CONF_INSTANCES, 1));
        container.deployVerticle("lbaas.HealthManagerVerticle", confHealthManager, confHealthManager.getInteger(CONF_INSTANCES, 1));

    }
}
