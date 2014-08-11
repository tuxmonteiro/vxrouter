/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

import static lbaas.Constants.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import lbaas.list.UniqueArrayList;
import lbaas.loadbalance.ILoadBalancePolicy;
import lbaas.loadbalance.impl.DefaultLoadBalancePolicy;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

public class Virtualhost extends JsonObject {

    private static final long serialVersionUID = -3715150640575829972L;

    private final String                  virtualhostName;
    private final UniqueArrayList<Client> endpoints;
    private final UniqueArrayList<Client> badEndpoints;
    private final Vertx                   vertx;

    private ILoadBalancePolicy connectPolicy     = null;
    private ILoadBalancePolicy persistencePolicy = null;

    public Virtualhost(String virtualhostName, final Vertx vertx) {
        super();
        this.virtualhostName = virtualhostName;
        this.endpoints = new UniqueArrayList<Client>();
        this.badEndpoints = new UniqueArrayList<Client>();
        this.vertx = vertx;
    }

    @Override
    public String toString() {
        return getVirtualhostName();
    }

    public boolean addClient(String endpoint, boolean endPointOk) {
        if (endPointOk) {
            putBoolean(transientStateFieldName, true);
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
            putBoolean(transientStateFieldName, true);
            return endpoints.remove(new Client(endpoint, vertx));
        } else {
            return badEndpoints.remove(new Client(endpoint, vertx));
        }
    }

    public void clear(boolean endPointOk) {
        if (endPointOk) {
            endpoints.clear();
            putBoolean(transientStateFieldName, true);
        } else {
            badEndpoints.clear();
        }
    }

    public void clearAll() {
        endpoints.clear();
        badEndpoints.clear();
        putBoolean(transientStateFieldName, true);
    }

    public Client getChoice(RequestData requestData) {
        // Default: isNewConnection = true
        return getChoice(requestData, true);
    }

    public Client getChoice(RequestData requestData, boolean isNewConnection) {
        requestData.setProperties(this);
        Client chosen;
        if (isNewConnection) {
            if (connectPolicy==null) {
                getLoadBalancePolicy();
            }
            chosen = connectPolicy.getChoice(endpoints, requestData);
        } else {
            if (persistencePolicy==null) {
                getPersistencePolicy();
            }
            chosen = persistencePolicy.getChoice(endpoints, requestData);
        }
        return chosen;
    }

    public ILoadBalancePolicy getLoadBalancePolicy() {
        String loadBalancePolicyStr = getString(loadBalancePolicyFieldName, defaultLoadBalancePolicy);
        connectPolicy = loadBalancePolicyClassLoader(loadBalancePolicyStr);
        if (connectPolicy.isDefault()) {
            putString(loadBalancePolicyFieldName, connectPolicy.toString());
        }
        return connectPolicy;
    }

    public ILoadBalancePolicy getPersistencePolicy() {
        String persistencePolicyStr = getString(persistencePolicyFieldName, defaultLoadBalancePolicy);
        persistencePolicy = loadBalancePolicyClassLoader(persistencePolicyStr);
        if (persistencePolicy.isDefault()) {
            putString(persistencePolicyFieldName, persistencePolicy.toString());
        }
        return persistencePolicy;
    }

    public ILoadBalancePolicy loadBalancePolicyClassLoader(String loadBalancePolicyName) {
        try {

            @SuppressWarnings("unchecked")
            Class<ILoadBalancePolicy> classLoader = (Class<ILoadBalancePolicy>) Class.forName(
                            String.format("%s.%s", packageOfLoadBalancePolicyClasses, loadBalancePolicyName));
            Constructor<ILoadBalancePolicy> classPolicy = classLoader.getConstructor();

            return classPolicy.newInstance();

        } catch (   ClassNotFoundException |
                    InstantiationException |
                    IllegalAccessException |
                    IllegalArgumentException |
                    InvocationTargetException |
                    NoSuchMethodException |
                    SecurityException e1 ) {
            ILoadBalancePolicy defaultLoadBalance = new DefaultLoadBalancePolicy();
            return defaultLoadBalance;
        }
    }

    public boolean hasClients() {
        return !endpoints.isEmpty();
    }

    public boolean hasBadClients() {
        return !badEndpoints.isEmpty();
    }

}
