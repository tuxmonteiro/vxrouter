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

public interface ICounter {

    public abstract void httpCode(String key, Integer code);

    public abstract void incrHttpCode(String key, Integer code);

    public abstract void incrHttpCode(String key, Integer code, double sample);

    public abstract void decrHttpCode(String key, Integer code);

    public abstract void decrHttpCode(String key, Integer code, double sample);

    public abstract void requestTime(String key, Long initialRequestTime);

    public abstract void sendActiveSessions(String key, Long initialRequestTime);

    public String cleanupString(String aString, String strDefault);

}
