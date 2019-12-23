package com.yit.deploy.core.model;

public class StatusCode implements JsonSupport<StatusCode> {
    private int code;
    private String message;

    public StatusCode() {}

    public StatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean hasError() {
        return code != 200;
    }

    public boolean succeeded() {
        return code == 200;
    }

    public static final StatusCode OK = new StatusCode(200, "OK");
}
