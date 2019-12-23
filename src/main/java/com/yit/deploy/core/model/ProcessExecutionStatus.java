package com.yit.deploy.core.model;

import com.yit.deploy.core.utils.Utils;

public class ProcessExecutionStatus {
    private ProcessLauncher launcher;
    private byte[] stdout;
    private byte[] stderr;
    private int code;

    public String getText() {
        return new String(stdout, Utils.DefaultCharset);
    }

    public String getError() {
        return new String(stderr, Utils.DefaultCharset);
    }

    public ProcessLauncher getLauncher() {
        return launcher;
    }

    public void setLauncher(ProcessLauncher launcher) {
        this.launcher = launcher;
    }

    public byte[] getStdout() {
        return stdout;
    }

    public void setStdout(byte[] stdout) {
        this.stdout = stdout;
    }

    public byte[] getStderr() {
        return stderr;
    }

    public void setStderr(byte[] stderr) {
        this.stderr = stderr;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}