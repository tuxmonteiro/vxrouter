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
package lbaas.metrics;

import static org.vertx.java.core.datagram.InternetProtocolFamily.IPv4;
import static lbaas.core.Constants.CONF_STATSD_ENABLE;
import static lbaas.core.Constants.CONF_STATSD_HOST;
import static lbaas.core.Constants.CONF_STATSD_PORT;
import static lbaas.core.Constants.CONF_STATSD_PREFIX;

import lbaas.metrics.StatsdClient.TypeStatsdMessage;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class CounterWithStatsd implements ICounter {
    private final StatsdClient statsdClient;

    public CounterWithStatsd(final JsonObject conf, final Vertx vertx, final Logger log) {
        if (conf.getBoolean(CONF_STATSD_ENABLE, false)) {
            String statsdHost = conf.getString(CONF_STATSD_HOST,"127.0.0.1");
            Integer statsdPort = conf.getInteger(CONF_STATSD_PORT, 8125);
            String statsdPrefix = conf.getString(CONF_STATSD_PREFIX,"");
            final DatagramSocket dgram = vertx.createDatagramSocket(IPv4).setReuseAddress(true);
            statsdClient = new StatsdClient(statsdHost, statsdPort, statsdPrefix, dgram, log);
        } else {
            statsdClient = null;
        }
    }

    @Override
    public void httpCode(String key, Integer code) {
        if (statsdClient!=null && key!=null && !("".equals(key))) {
            statsdClient.send(TypeStatsdMessage.TIME,
                    String.format("%s.httpCode%d:%d", key, code, 1));
        }
    }

    @Override
    public void incrHttpCode(String key, Integer code) {
        incrHttpCode(key, code, 1.0);
    }

    @Override
    public void incrHttpCode(String key, Integer code, double sample) {
        String srtSample = sample > 0.0 && sample < 1.0 ? String.format("|@%d", sample) : "";
        if (statsdClient!=null && key!=null && !("".equals(key))) {
            statsdClient.send(TypeStatsdMessage.COUNT,
                    String.format("%s.httpCode%d:%d%s", key, code, 1, srtSample));
        }
    }

    @Override
    public void decrHttpCode(String key, Integer code) {
        decrHttpCode(key, code, 1.0);
    }

    @Override
    public void decrHttpCode(String key, Integer code, double sample) {
        String srtSample = sample > 0.0 && sample < 1.0 ? String.format("|@%d", sample) : "";
        if (statsdClient!=null && key!=null && !("".equals(key))) {
            statsdClient.send(TypeStatsdMessage.COUNT,
                    String.format("%s.httpCode%d:%d%s", key, code, -1, srtSample));
        }
    }

    @Override
    public void requestTime(String key, final Long initialRequestTime) {
        Long requestTime = System.currentTimeMillis() - initialRequestTime;
        if (statsdClient!=null && key!=null && !("".equals(key))) {
            statsdClient.send(TypeStatsdMessage.TIME,
                    String.format("%s.requestTime:%d", key, requestTime));
        }
    }

    @Override
    public void sendActiveSessions(String key, Long initialRequestTime) {
        if (statsdClient!=null && key!=null && !("".equals(key))) {
            statsdClient.send(TypeStatsdMessage.COUNT, String.format("%s.active:%d", key, 1));
        }
    }

    @Override
    public String cleanupString(String aString, String strDefault) {
        return !"".equals(aString)?aString.replaceAll("[^\\w]", "_"):strDefault;
    }

}
