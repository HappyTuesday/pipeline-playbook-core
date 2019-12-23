package com.yit.deploy.core.exceptions;

/**
 * Created by nick on 19/09/2017.
 */
public class UnsupportedPlatformException extends RuntimeException {
    public UnsupportedPlatformException(String uname, String message) {
        super("unsupported platform " + uname + ". " + message);
    }
}
