package lbaas;

import lbaas.list.UniqueArrayList;

import org.vertx.java.core.Vertx;

public class Virtualhost {

    private final String virtualhostName;
    private UniqueArrayList<Client> endpoints;
    private UniqueArrayList<Client> badEndpoints;
    private Vertx vertx;

    public Virtualhost(String virtualhostName, final Vertx vertx) {
        this.virtualhostName = virtualhostName;
        this.endpoints = new UniqueArrayList<Client>();
        this.badEndpoints = new UniqueArrayList<Client>();
        this.vertx = vertx;
    }

    public boolean addClient(String endpoint, boolean endPointOk) {
        if (endPointOk) {
            return endpoints.add(new Client(endpoint, vertx));
        } else {
            return badEndpoints.add(new Client(endpoint, vertx));
        }
    }

    public UniqueArrayList<Client> getClients(boolean endPointOk) {
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

	public void clear(boolean endPointOk) {
		if (endPointOk) {
            endpoints.clear();
        } else {
            badEndpoints.clear();
        }
	}

}
