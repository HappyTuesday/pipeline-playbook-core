package com.yit.deploy.core.exceptions;

public class DisabledPlayException extends RuntimeException {
    private String playName;

    public DisabledPlayException(String playName) {
        super("play " + playName + " is disabled ");
        this.playName = playName;
    }

    public String getPlayName() {
        return playName;
    }
}
