package com.yit.deploy.core.info;

import com.yit.deploy.core.algorithm.Merger;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.ConnectionChannel;
import com.yit.deploy.core.records.HostRecord;
import com.yit.deploy.core.records.RecordTarget;

import java.util.*;

public class HostInfo implements Merger<HostInfo>, RecordTarget<HostRecord> {
    private final String name;
    private String id;
    private String user;
    private Integer port;
    private ConnectionChannel channel;
    private Boolean retired;
    private Map<String, Object> labels = new LinkedHashMap<>();
    private String description;

    public HostInfo(String name) {
        this.name = name;
    }

    public HostInfo(HostInfo that) {
        this.name = that.name;
        this.id = that.id;
        this.user = that.user;
        this.port = that.port;
        this.channel = that.channel;
        this.retired = that.retired;
        this.labels = new LinkedHashMap<>(that.labels);
        this.description = that.description;
    }

    /**
     * merge that instance into this instance
     *
     * @param that
     */
    @Override
    public void merge(HostInfo that) {
        if (that.id != null) this.id = that.id;
        if (that.user != null) this.user = that.user;
        if (that.port != null) this.port = that.port;
        if (that.channel != null) this.channel = that.channel;
        if (that.retired != null) this.retired = that.retired;
        if (that.labels != null) {
            this.labels.putAll(that.labels);
        }
        if (that.description != null) {
            this.description = that.description;
        }
    }

    /**
     * apply record to target
     *
     * @param record record to apply
     */
    @Override
    public HostInfo withRecord(HostRecord record) {
        HostInfo target = new HostInfo(this);
        target.id = record.getId();
        if (record.getUser() != null) {
            target.user = record.getUser();
        }
        if (record.getPort() != null) {
            target.port = record.getPort();
        }
        if (record.getChannel() != null) {
            target.channel = record.getChannel();
        }
        if (record.getRetired() != null) {
            target.retired = record.getRetired();
        }
        if (record.getLabels() != null) {
            target.labels = new LinkedHashMap<>(record.getLabels());
        }
        if (record.getDescription() != null) {
            target.description = record.getDescription();
        }
        return target;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public Boolean getRetired() {
        return retired;
    }

    public Map<String, Object> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, Object> labels) {
        this.labels = labels;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
