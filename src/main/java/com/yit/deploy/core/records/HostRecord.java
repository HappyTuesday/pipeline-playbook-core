package com.yit.deploy.core.records;

import com.yit.deploy.core.model.ConnectionChannel;

import java.util.LinkedHashMap;
import java.util.Map;

public class HostRecord implements Record {
    private long commitId;
    private String env;
    private String name;
    private String user;
    private Integer port;
    private ConnectionChannel channel;
    private Boolean retired;
    private boolean disabled;
    private LinkedHashMap<String, Object> labels;
    private String description;

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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public ConnectionChannel getChannel() {
        return channel;
    }

    public void setChannel(ConnectionChannel channel) {
        this.channel = channel;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public LinkedHashMap<String, Object> getLabels() {
        return labels;
    }

    public void setLabels(LinkedHashMap<String, Object> labels) {
        this.labels = labels;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
