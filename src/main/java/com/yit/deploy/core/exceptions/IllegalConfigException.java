package com.yit.deploy.core.exceptions;

public class IllegalConfigException extends RuntimeException {
    public IllegalConfigException() {}

    public IllegalConfigException(String message) {
        super(message);
    }

    public IllegalConfigException(Throwable t) {
        super(t);
    }

    public IllegalConfigException(String message, Throwable t) {
        super(message, t);
    }
}
