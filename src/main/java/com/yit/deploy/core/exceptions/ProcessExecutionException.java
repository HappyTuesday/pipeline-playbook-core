package com.yit.deploy.core.exceptions;

import com.yit.deploy.core.model.ProcessExecutionStatus;

public class ProcessExecutionException extends RuntimeException {
    private final String command;
    private final String error;
    private final int code;

    public ProcessExecutionException(String command, String error, int code) {
        super("process exited with code " + code + ": " + error);
        this.command = command;
        this.error = error;
        this.code = code;
    }

    public ProcessExecutionException(ProcessExecutionStatus status) {
        this(status.getLauncher() == null ? "bash" : String.join(" ", status.getLauncher().getCmd()), status.getText() + " " + status.getError(), status.getCode());
    }

    public String getError() {
        return error;
    }

    public int getCode() {
        return code;
    }

    public String getCommand() {
        return command;
    }
}