/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package lbaas;

import lbaas.util.HashAlgorithm;

public class Constants {

    private Constants() {
    }

    public static final String QUEUE_ROUTE_ADD                   = "route.add";
    public static final String QUEUE_ROUTE_DEL                   = "route.del";
    public static final String QUEUE_ROUTE_VERSION               = "route.version";
    public static final String QUEUE_HEALTHCHECK_OK              = "healthcheck.ok";
    public static final String QUEUE_HEALTHCHECK_FAIL            = "healthcheck.fail";
    public static final String QUEUE_BACKEND_CONNECTIONS_PREFIX  = "conn_";

    public static final String CONF_INSTANCES                    = "instances";
    public static final String CONF_PORT                         = "port";
    public static final String CONF_ENABLE_ACCESSLOG             = "enableAccessLog";

    public static final String loadBalancePolicyFieldName        = "loadBalancePolicy";
    public static final String persistencePolicyFieldName        = "persistencePolicy";
    public static final String cacheTimeOutFieldName             = "cacheTimeout";
    public static final String hashAlgorithmFieldName            = "hashAlgorithm";
    public static final String transientStateFieldName           = "transientState";
    public static final String uuidFieldName                     = "uuid";
    public static final String numConnectionFieldName            = "numConnections";

    public static final String defaultHashAlgorithm              = HashAlgorithm.HashType.SIP24.toString();


    public static final String defaultLoadBalancePolicy          = "DefaultLoadBalancePolicy";
    public static final String packageOfLoadBalancePolicyClasses = "lbaas.loadbalance.impl";

}
