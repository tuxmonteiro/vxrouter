/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */

package lbaas;

public interface ICounter {

    public abstract void httpCode(String key, Integer code);

    public abstract void incrHttpCode(String key, Integer code);

    public abstract void incrHttpCode(String key, Integer code, double sample);

    public abstract void decrHttpCode(String key, Integer code);

    public abstract void decrHttpCode(String key, Integer code, double sample);

    public abstract void requestTime(String key, Long initialRequestTime);

    public abstract void incrActiveSessions(String key);

    public abstract void decrActiveSessions(String key);

    public String cleanupString(String aString, String strDefault);

}
