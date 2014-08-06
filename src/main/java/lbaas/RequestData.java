package lbaas;

import java.net.URI;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerRequest;

public class RequestData {

    final MultiMap headers;
    final MultiMap params;
    final URI uri;
    final String remoteAddress;
    final String remotePort;

    public RequestData(HttpServerRequest request) {
        if (request!=null) {
            this.headers = request.headers();
            this.params = request.params();
            this.uri = request.absoluteURI();
            this.remoteAddress = request.netSocket().remoteAddress().getHostString();
            this.remotePort = String.format("%d", request.netSocket().remoteAddress().getPort());
        } else {
            this.headers = new CaseInsensitiveMultiMap();
            this.params = new CaseInsensitiveMultiMap();
            this.uri = null;
            this.remoteAddress = "";
            this.remotePort = "";
        }
    }

    public MultiMap getHeaders() {
        return headers;
    }

    public MultiMap getParams() {
        return params;
    }

    public URI getUri() {
        return uri;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getRemotePort() {
        return remotePort;
    }

    public String getFrontEnd() {
        return headers.contains("Host") ? headers.get("Host") : "";
    }

}
