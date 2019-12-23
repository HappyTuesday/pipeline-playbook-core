package com.yit.deploy.core.records;

import java.util.List;

public class HostGroupRecord implements Record {
    private long commitId;
    private String env;
    private String name;
    private String description;
    private Boolean overrideHosts;
    private List<String> hosts;
    private List<String> hostsRetired;
    private List<String> inherits;
    private List<String> inheritsRetired;
    private boolean disabled;

    public long getCommitId() {
        return commitId;
    }

    public void setCommitId(long commitId) {
        this.commitId = commitId;
    }

    public String getEnv() {
        return env;
    }

    public String getName() {
        return name;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public String getId() {
        return env + ":" + name;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getOverrideHosts() {
        return overrideHosts;
    }

    public void setOverrideHosts(Boolean overrideHosts) {
        this.overrideHosts = overrideHosts;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public List<String> getHostsRetired() {
        return hostsRetired;
    }

    public void setHostsRetired(List<String> hostsRetired) {
        this.hostsRetired = hostsRetired;
    }

    public List<String> getInherits() {
        return inherits;
    }

    public void setInherits(List<String> inherits) {
        this.inherits = inherits;
    }

    public List<String> getInheritsRetired() {
        return inheritsRetired;
    }

    public void setInheritsRetired(List<String> inheritsRetired) {
        this.inheritsRetired = inheritsRetired;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
