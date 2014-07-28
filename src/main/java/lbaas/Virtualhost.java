package lbaas;

import java.util.HashSet;
import java.util.Set;

import org.vertx.java.core.Vertx;

public class Virtualhost {

    private final String virtualhostName;
    private Set<Client> endpoints;
    private Set<Client> badEndpoints;
    private Vertx vertx;

    public Virtualhost(String virtualhostName, final Vertx vertx) {
        this.virtualhostName = virtualhostName;
        this.endpoints = new HashSet<Client>();
        this.badEndpoints = new HashSet<Client>();
        this.vertx = vertx;
    }

    public boolean addClient(String endpoint, boolean endPointOk) {
        if (endPointOk) {
            return endpoints.add(new Client(endpoint, vertx));
        } else {
            return badEndpoints.add(new Client(endpoint, vertx));
        }
    }

    public Set<Client> getClients(boolean endPointOk) {
        return endPointOk ? endpoints: badEndpoints;
    }

    public String getVirtualhostName() {
        return virtualhostName;
    }

    public Boolean removeClient(String endpoint, boolean endPointOk) {
        if (endPointOk) {
            return endpoints.remove(new Client(endpoint, vertx));
        } else {
            return badEndpoints.remove(new Client(endpoint, vertx));
        }
    }

}
