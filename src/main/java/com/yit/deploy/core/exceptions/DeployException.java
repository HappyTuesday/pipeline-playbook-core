package com.yit.deploy.core.exceptions;

import com.yit.deploy.core.model.StatusCode;

public class DeployException extends RuntimeException {
    private StatusCode statusCode;

    public DeployException(int code, String message) {
        this(new StatusCode(code, message));
    }

    public DeployException(StatusCode statusCode) {
        super("ERROR " + statusCode.getCode() + ": " + statusCode.getMessage());
        this.statusCode = statusCode;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }
}
