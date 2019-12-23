package com.yit.deploy.core.dsl.execute;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.support.StepsSupportWithHost;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;

import java.util.List;

public class HostExecutionContext extends PlayExecutionContext implements StepsSupportWithHost {

    private final Host host;
    private final List<Host> hostsInGroup;
    private final Variables hostWritable;

    public HostExecutionContext(HostExecutionContext cx) {
        this(cx, cx.host, cx.hostsInGroup, cx.hostWritable);
    }

    public HostExecutionContext(PlayExecutionContext pcx, Host host, List<Host> hostsInGroup, Variables hostWritable) {
        super(pcx, pcx.play);
        this.host = host;
        this.hostsInGroup = hostsInGroup;
        this.hostWritable = hostWritable;
        resolveWritableVars(hostWritable);
    }

    @Override
    public HostExecutionContext withJob(Job job) {
        return new HostExecutionContext(super.withJob(job), host, hostsInGroup, new SimpleVariables());
    }

    public TaskExecutionContext toTask(Task task) {
        return new TaskExecutionContext(this, task);
    }

    public Host getTargetHost() {
        return host;
    }

    public Host getCurrentHost() {
        return host;
    }

    public Host getCURRENT_HOST() {
        return getCurrentHost();
    }

    public String getCurrentHostName() {
        return host.getHostname();
    }

    public String getCURRENT_HOSTNAME() {
        return getCurrentHostName();
    }

    public String getANSIBLE_HOSTNAME() {
        return getCurrentHostName();
    }

    public String getansible_hostname() {
        return getCurrentHostName();
    }

    public String getCurrentHostIPAddress() {
        return host.getName();
    }

    public String getCURRENT_HOST_IP_ADDRESS() {
        return getCurrentHostIPAddress();
    }

    public String getINVENTORY_HOSTNAME() {
        return host.getName();
    }

    public String getinventory_hostname() {
        return host.getName();
    }

    public int getCurrentHostIndex() {
        return Lambda.findIndexOf(hostsInGroup, h -> h.getName().equals(host.getName()));
    }

    public int getCURRENT_HOST_INDEX() {
        return getCurrentHostIndex();
    }

    public List<Host> getHostsInGroup() {
        return hostsInGroup;
    }

    public List<Host> getHOSTS_IN_GROUP() {
        return getHostsInGroup();
    }

    public boolean isSingleTaskMode() {
        return getVariableOrDefault("single_task_mode", false);
    }
}
