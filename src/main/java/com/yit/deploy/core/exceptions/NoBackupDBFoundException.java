package com.yit.deploy.core.exceptions;

/**
 * Created by nick on 12/09/2017.
 */
public class NoBackupDBFoundException extends RuntimeException {
    public NoBackupDBFoundException(String message) {
        super(message);
    }
}
