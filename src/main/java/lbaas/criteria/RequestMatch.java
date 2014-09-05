package lbaas.criteria;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

public class RequestMatch implements IWhenMatch {

    private final HttpServerRequest req;

    public RequestMatch() {
        this(null);
    }

    public RequestMatch(HttpServerRequest req) {
        this.req = req;
    }

    @Override
    public String getHeader(String header) {
        MultiMap headers = req.headers();
        return headers.contains(header) ? headers.get(header) : null;
    }

    @Override
    public String getParam(String param) {
        MultiMap params = req.params();
        return params.contains(param) ? params.get(param) : null;
    }

    @Override
    public boolean isNull() {
        return req == null;
    }
}
