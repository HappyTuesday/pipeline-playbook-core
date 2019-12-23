package com.yit.deploy.core.exceptions;

/**
 * Created by nick on 19/12/2017.
 */
public class ContinueWaitException extends RuntimeException {
    public ContinueWaitException(String message) {
        this(message, null);
    }

    public ContinueWaitException(String message, Exception cause) {
        super(message, cause);
    }
}
