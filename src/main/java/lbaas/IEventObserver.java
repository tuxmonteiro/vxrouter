/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas;

public interface IEventObserver {
    public void setVersion(Long version);
    public void postAddEvent(String message);
    public void postDelEvent(String message);
}
