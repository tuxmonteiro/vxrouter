package lbaas;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import lbaas.list.UniqueArrayList;
import lbaas.loadbalance.ILoadBalancePolicy;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class Virtualhost {

    private final String virtualhostName;
    private UniqueArrayList<Client> endpoints;
    private UniqueArrayList<Client> badEndpoints;
    private Vertx vertx;
    private RequestData requestData = null;
    private ILoadBalancePolicy connectPolicy;
    private ILoadBalancePolicy persistencePolicy;
    private JsonObject properties = new JsonObject();
    private final String loadBalancePolicyFieldName = "loadBalancePolicy";
    private final String persistencePolicyFieldName = "persistencePolicy";
    private final String defaultLoadBalancePolicy = "DefaultLoadBalancePolicy";
    private final String packageOfLoadBalancePolicyClasses = "lbaas.loadbalance.impl";

    public Virtualhost(String virtualhostName, final Vertx vertx) {
        this.virtualhostName = virtualhostName;
        this.endpoints = new UniqueArrayList<Client>();
        this.badEndpoints = new UniqueArrayList<Client>();
        this.vertx = vertx;
    }

    public Virtualhost setRequestData(RequestData requestData) {
        this.requestData = requestData;
        return this;
    }

    public JsonObject getProperties() {
        return properties;
    }

    public void setProperties(JsonObject properties) {
        this.properties = properties;
    }

    public String getLoadBalancePolicyFieldName() {
        return loadBalancePolicyFieldName;
    }

    public String getPersistencePolicyFieldName() {
        return persistencePolicyFieldName;
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

    public Client getChoice() {
        // Default: isNewConnection = true
        return getChoice(true);
    }

    public Client getChoice(boolean isNewConnection) {
        if (isNewConnection) {
            if (connectPolicy==null) {
                getLoadBalancePolicy();
            }
            return connectPolicy.getChoice(endpoints, requestData);
        } else {
            if (persistencePolicy==null) {
                getPersistencePolicy();
            }
            return persistencePolicy.getChoice(endpoints, requestData);
        }
    }

    public ILoadBalancePolicy getLoadBalancePolicy() {
        String loadBalancePolicyStr = properties.getString(loadBalancePolicyFieldName, defaultLoadBalancePolicy);
        connectPolicy = loadBalancePolicyClassLoader(loadBalancePolicyStr);
        return connectPolicy;
    }

    public ILoadBalancePolicy getPersistencePolicy() {
        String persistencePolicyStr = properties.getString(persistencePolicyFieldName, defaultLoadBalancePolicy);
        persistencePolicy = loadBalancePolicyClassLoader(persistencePolicyStr);
        return persistencePolicy;
    }

    public ILoadBalancePolicy loadBalancePolicyClassLoader(String loadBalancePolicyName) {
        try {

            @SuppressWarnings("unchecked")
            Class<ILoadBalancePolicy> classLoader = (Class<ILoadBalancePolicy>) Class.forName(
                            String.format("%s.%s", packageOfLoadBalancePolicyClasses, loadBalancePolicyName));
            Constructor<ILoadBalancePolicy> classPersistencePolicy =
                    classLoader.getConstructor();

            return classPersistencePolicy.newInstance();

        } catch (   ClassNotFoundException |
                    InstantiationException |
                    IllegalAccessException |
                    IllegalArgumentException |
                    InvocationTargetException |
                    NoSuchMethodException |
                    SecurityException e1 ) {
//            log.error(String.format("[%s] LoadBalancePolicy Problem. Using DefaultLoadBalancePolicy. Message: %s", verticleId, e1.getMessage()));
//            return new DefaultLoadBalancePolicy();
            return null;
        }
    }

    public void clearProperties() {
        properties = new JsonObject();
    }

}
