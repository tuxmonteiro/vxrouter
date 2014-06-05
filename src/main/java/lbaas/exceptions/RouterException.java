/*
 * Copyright (c) 2014 The original author or authors.
 * All rights reserved.
 */
package lbaas.exceptions;

public class RouterException extends RuntimeException {

    private static final long serialVersionUID = -2881325143281920669L;

    public RouterException(String message) {
        super(message);
    }

    public RouterException() {
        super();
    }
}
