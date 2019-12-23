package com.yit.deploy.core.info;

import com.yit.deploy.core.algorithm.Merger;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.records.HostGroupRecord;
import com.yit.deploy.core.records.RecordTarget;

import java.util.ArrayList;
import java.util.List;

public class HostGroupInfo implements Merger<HostGroupInfo>, RecordTarget<HostGroupRecord> {
    private String name;
    private String id;
    private String description;
    private boolean overrideHosts;
    private List<String> hosts = new ArrayList<>();
    private List<String> hostsRetired = new ArrayList<>();
    private List<String> inherits = new ArrayList<>();
    private List<String> inheritsRetired = new ArrayList<>();

    public HostGroupInfo() {}

    public HostGroupInfo(HostGroupInfo that) {
        this.name = that.name;
        this.id = that.id;
        this.description = that.description;
        this.overrideHosts = that.overrideHosts;
        this.hosts = that.hosts;
        this.hostsRetired = that.hostsRetired;
        this.inherits = that.inherits;
        this.inheritsRetired = that.inheritsRetired;
    }

    public HostGroupInfo(String name) {
        this.name = name;
    }

    /**
     * merge that instance into this instance
     *
     * @param that
     */
    @Override
    public void merge(HostGroupInfo that) {
        if (that.id != null) {
            this.id = that.id;
        }

        this.description = that.description;
        if (that.overrideHosts) {
            this.hosts.clear();
            this.hostsRetired.clear();
        }

        Lambda.uniqueAdd(this.hosts, that.hosts);
        Lambda.uniqueAdd(this.hostsRetired, that.hostsRetired);
        Lambda.uniqueAdd(this.inherits, that.inherits);
        Lambda.uniqueAdd(this.inheritsRetired, that.inheritsRetired);
    }

    /**
     * apply record to target
     *
     * @param record record to apply
     */
    @Override
    public HostGroupInfo withRecord(HostGroupRecord record) {
        HostGroupInfo target = new HostGroupInfo(this);
        target.id = record.getId();
        if (record.getDescription() != null) {
            target.description = record.getDescription();
        }
        if (record.getOverrideHosts() != null) {
            target.overrideHosts = record.getOverrideHosts();
        }
        if (record.getHosts() != null) {
            target.hosts = record.getHosts();
        }
        if (record.getHostsRetired() != null) {
            target.hostsRetired = record.getHostsRetired();
        }
        if (record.getInherits() != null) {
            target.inherits = record.getInherits();
        }
        if (record.getInheritsRetired() != null) {
            target.inheritsRetired = record.getInheritsRetired();
        }
        return target;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOverrideHosts() {
        return overrideHosts;
    }

    public void setOverrideHosts(boolean overrideHosts) {
        this.overrideHosts = overrideHosts;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public List<String> getHostsRetired() {
        return hostsRetired;
    }

    public List<String> getInherits() {
        return inherits;
    }

    public List<String> getInheritsRetired() {
        return inheritsRetired;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HostGroupInfo && name.equals(((HostGroupInfo)obj).name);
    }
}
