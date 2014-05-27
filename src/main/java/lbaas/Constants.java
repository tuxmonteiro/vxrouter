package lbaas;

public class Constants {

    private Constants() {
    }

    public static final String QUEUE_ROUTE_ADD = "route.add";
    public static final String QUEUE_ROUTE_DEL = "route.del";
    public static final String QUEUE_ROUTE_VERSION = "route.version";
    public static final String QUEUE_HEALTHCHECK_OK = "healthcheck.ok";
    public static final String QUEUE_HEALTHCHECK_FAIL = "healthcheck.fail";

    public static final String CONF_INSTANCES  = "instances";
    public static final String CONF_PORT       = "port";

    public static final Character SEPARATOR = ':';
    public static final int NUM_FIELDS = 4;

}
