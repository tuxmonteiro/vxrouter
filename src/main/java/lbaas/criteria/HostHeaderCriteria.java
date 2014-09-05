package lbaas.criteria;

import java.util.Map;

import lbaas.core.Virtualhost;
import lbaas.logger.SafeLogger;

import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;

public class HostHeaderCriteria<T> implements ICriteria<T> {

    private final SafeLogger log = new SafeLogger();
    private String host = "";
    private Map<String, T> map = null;

    @Override
    public ICriteria<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    @Override
    public ICriteria<T> given(final Map<String, T> map) {
        this.map = map;
        return this;
    }

    @Override
    public ICriteria<T> when(final Object param) {
        if (param instanceof HttpServerRequest) {
            host = new RequestMatch((HttpServerRequest)param).getHeader(HttpHeaders.HOST.toString());
        } else {
            log.warn(String.format("Param is instance of %s.class. Expected %s.class",
                    param.getClass().getSimpleName(), HttpServerRequest.class.getSimpleName()));
        }
        return this;
    }

    @Override
    public T getResult() {
        if ("".equals(host)) {
            log.warn("Host UNDEF");
            return null;
        }
        String hostWithoutPort = host.split(":")[0];
        if (!map.containsKey(hostWithoutPort)) {
            log.warn(String.format("Host: %s UNDEF", hostWithoutPort));
            return null;
        }
        T result = map.get(hostWithoutPort);
        if (!(result instanceof Virtualhost)) {
            log.warn(String.format("Result is instance of %s.class. Expected %s.class",
                    result.getClass().getSimpleName(), Virtualhost.class.getSimpleName()));
            return null;
        }
        if (!((Virtualhost)result).hasBackends()) {
            log.warn(String.format("Host %s without backends", hostWithoutPort));
            return null;
        }

        return result;
    }
}
