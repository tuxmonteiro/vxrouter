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
package lbaas.core;

import java.net.InetSocketAddress;
import java.net.URI;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

public class RequestData {

    private final MultiMap headers;
    private final MultiMap params;
    private final URI uri;
    private String remoteAddress;
    private String remotePort;
    private JsonObject properties;

    public RequestData() {
        this(null, null, null, null, null);
    }

    public RequestData(final String remoteAddress,
                       final String remotePort) {
        this(null, null, null, remoteAddress, remotePort);
    }

    public RequestData(final MultiMap headers,
                       final MultiMap params,
                       final URI uri,
                       final String remoteAddress,
                       final String remotePort) {
        this.headers = headers;
        this.params = params;
        this.uri = uri;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
    }

    public RequestData(final HttpServerRequest request) {
        if (request!=null) {
            this.headers = request.headers();
            this.params = request.params();
            this.uri = request.absoluteURI();
            InetSocketAddress localRemoteAddress = request.remoteAddress();
            this.remoteAddress = localRemoteAddress.getHostString();
            this.remotePort = Integer.toString(localRemoteAddress.getPort());
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

    public String getHeaderHost() {
        return headers.contains("Host") ? headers.get("Host") : "";
    }

    public JsonObject getProperties() {
        return properties;
    }

    public void setProperties(final JsonObject properties) {
        this.properties = properties;
    }
}
