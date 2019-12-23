package com.yit.deploy.core.model;

import com.yit.deploy.core.function.Lambda;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class ContainerProcessLauncher implements Serializable {
    private String containerId;
    private List<String> cmd;

    @Deprecated
    public ContainerProcessLauncher() {}

    public ContainerProcessLauncher(String containerId, List<String> cmd) {
        this.containerId = containerId;
        this.cmd = cmd;
    }

    public ProcessLauncher getProcessLauncher() {
        return new ProcessLauncher(Lambda.concat(Arrays.asList("docker", "exec", "-i", containerId), this.cmd));
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public void setCmd(List<String> cmd) {
        this.cmd = cmd;
    }
}
