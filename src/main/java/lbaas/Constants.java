/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

public class Constants {

    private Constants() {
    }

    public static final String QUEUE_ROUTE_ADD = "route.add";
    public static final String QUEUE_ROUTE_DEL = "route.del";
    public static final String QUEUE_ROUTE_VERSION = "route.version";
    public static final String QUEUE_HEALTHCHECK_OK = "healthcheck.ok";
    public static final String QUEUE_HEALTHCHECK_FAIL = "healthcheck.fail";

    public static final String CONF_INSTANCES  = "instances";
    public static final String CONF_PORT       = "port";

    public static final Character SEPARATOR = '#';
    public static final int NUM_FIELDS = 6;
    public static String STRING_PATTERN = makeStringPattern();

    private static String makeStringPattern() {
        String tempStringPattern = "";
        for (int counter=1; counter<NUM_FIELDS; counter++) {
            tempStringPattern += "%s%s";
        }
        tempStringPattern += "%s";
        return tempStringPattern;
    }

}
