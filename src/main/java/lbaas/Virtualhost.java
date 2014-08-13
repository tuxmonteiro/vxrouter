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
    private final UniqueArrayList<Backend> backends;
    private final UniqueArrayList<Backend> badBackends;
    private final Vertx                   vertx;

    private ILoadBalancePolicy connectPolicy     = null;
    private ILoadBalancePolicy persistencePolicy = null;

    public Virtualhost(String virtualhostName, final Vertx vertx) {
        super();
        this.virtualhostName = virtualhostName;
        this.backends = new UniqueArrayList<Backend>();
        this.badBackends = new UniqueArrayList<Backend>();
        this.vertx = vertx;
    }

    @Override
    public String toString() {
        return getVirtualhostName();
    }

    public boolean addBackend(String backend, boolean backendOk) {
        if (backendOk) {
            putBoolean(transientStateFieldName, true);
            return backends.add(new Backend(backend, vertx));
        } else {
            return badBackends.add(new Backend(backend, vertx));
        }
    }

    public UniqueArrayList<Backend> getBackends(boolean backendOk) {
        return backendOk ? backends: badBackends;
    }

    public String getVirtualhostName() {
        return virtualhostName;
    }

    public Boolean removeBackend(String backend, boolean backendOk) {
        if (backendOk) {
            putBoolean(transientStateFieldName, true);
            return backends.remove(new Backend(backend, vertx));
        } else {
            return badBackends.remove(new Backend(backend, vertx));
        }
    }

    public void clear(boolean backendOk) {
        if (backendOk) {
            backends.clear();
            putBoolean(transientStateFieldName, true);
        } else {
            badBackends.clear();
        }
    }

    public void clearAll() {
        backends.clear();
        badBackends.clear();
        putBoolean(transientStateFieldName, true);
    }

    public Backend getChoice(RequestData requestData) {
        // Default: isNewConnection = true
        return getChoice(requestData, true);
    }

    public Backend getChoice(RequestData requestData, boolean isNewConnection) {
        requestData.setProperties(this);
        Backend chosen;
        if (isNewConnection) {
            if (connectPolicy==null) {
                getLoadBalancePolicy();
            }
            chosen = connectPolicy.getChoice(backends, requestData);
        } else {
            if (persistencePolicy==null) {
                getPersistencePolicy();
            }
            chosen = persistencePolicy.getChoice(backends, requestData);
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

    public boolean hasBackends() {
        return !backends.isEmpty();
    }

    public boolean hasBadBackends() {
        return !badBackends.isEmpty();
    }

}
