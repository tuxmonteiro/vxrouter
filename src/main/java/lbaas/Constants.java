/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

public class Constants {

    private Constants() {
    }

    public static final String QUEUE_ROUTE_ADD                   = "route.add";
    public static final String QUEUE_ROUTE_DEL                   = "route.del";
    public static final String QUEUE_ROUTE_VERSION               = "route.version";
    public static final String QUEUE_HEALTHCHECK_OK              = "healthcheck.ok";
    public static final String QUEUE_HEALTHCHECK_FAIL            = "healthcheck.fail";

    public static final String CONF_INSTANCES                    = "instances";
    public static final String CONF_PORT                         = "port";

    public static final String loadBalancePolicyFieldName        = "loadBalancePolicy";
    public static final String persistencePolicyFieldName        = "persistencePolicy";
    public static final String cacheTimeOutFieldName             = "cacheTimeout";
    public static final String hashAlgorithmFieldName            = "hashAlgorithm";
    public static final String transientStateFieldName           = "transientState";

    public static final String defaultLoadBalancePolicy          = "DefaultLoadBalancePolicy";
    public static final String packageOfLoadBalancePolicyClasses = "lbaas.loadbalance.impl";

}
