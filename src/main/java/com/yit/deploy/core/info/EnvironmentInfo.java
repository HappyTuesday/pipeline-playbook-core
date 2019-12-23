package com.yit.deploy.core.info;

import com.yit.deploy.core.inherits.Inherits;
import com.yit.deploy.core.records.*;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.SimpleVariables;

import java.util.*;

public class EnvironmentInfo implements RecordTarget<EnvironmentRecord> {
    private String name;
    private String id;
    private Boolean abstracted;
    private String description;
    private List<String> parents = new ArrayList<>();
    private SimpleVariables vars = new SimpleVariables();
    private List<String> labels = new ArrayList<>();
    private Map<String, HostInfo> hosts = new HashMap<>();
    private Map<String, HostGroupInfo> hostGroups = new HashMap<>();

    public EnvironmentInfo() {
    }

    public EnvironmentInfo(String name) {
        this.name = name;
    }

    public EnvironmentInfo(EnvironmentInfo that) {
        this.name = that.name;
        this.id = that.id;
        this.abstracted = that.abstracted;
        this.description = that.description;
        this.parents = that.parents;
        this.vars = that.vars;
        this.labels = that.labels;
        this.hosts = that.hosts;
        this.hostGroups = that.hostGroups;
    }

    @Override
    public EnvironmentInfo withRecord(EnvironmentRecord record) {
        EnvironmentInfo target = new EnvironmentInfo(this);
        target.id = record.getId();
        if (record.getAbstracted() != null) {
            target.abstracted = record.getAbstracted();
        }
        if (record.getDescription() != null) {
            target.description = record.getDescription();
        }
        if (record.getParents() != null) {
            target.parents = record.getParents();
        }
        if (record.getLabels() != null) {
            target.labels = record.getLabels();
        }
        return target;
    }

    public EnvironmentInfo withHostRecords(List<HostRecord> records) {
        EnvironmentInfo target = new EnvironmentInfo(this);
        target.hosts = new HashMap<>(target.hosts);
        RecordTarget.applyRecordsToMap(records, HostRecord::getName, target.hosts, r -> new HostInfo(r.getName()));
        return target;
    }

    public EnvironmentInfo withHostGroupRecords(List<HostGroupRecord> records) {
        EnvironmentInfo target = new EnvironmentInfo(this);
        target.hostGroups = new HashMap<>(target.hostGroups);
        RecordTarget.applyRecordsToMap(records, HostGroupRecord::getName, target.hostGroups, r -> new HostGroupInfo(r.getName()));
        return this;
    }

    public EnvironmentInfo withAssignRecords(List<Assignment> assigns) {
        List<VariableInfo> list = this.vars.toInfo();
        for (Assignment assign : assigns) {
            assign.insertToVariableInfoList(list);
        }

        EnvironmentInfo target = new EnvironmentInfo(this);
        target.vars = new SimpleVariables(list);
        return target;
    }

    /**
     * the environment name
     */
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

    public Boolean isAbstracted() {
        return abstracted;
    }

    public void setAbstracted(Boolean abstracted) {
        this.abstracted = abstracted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getParents() {
        return parents;
    }

    public SimpleVariables getVars() {
        return vars;
    }

    /**
     * a set of labels assigned to this environment,
     * used for environment query and filter
     */
    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    /**
     * all hosts defined in this environment
     */
    public Map<String, HostInfo> getHosts() {
        return hosts;
    }

    public void setHosts(Map<String, HostInfo> hosts) {
        this.hosts = hosts;
    }

    /**
     * all host groups defined in this environment
     * the hosts contained in this hostGroups is equal to the hosts field
     */
    public Map<String, HostGroupInfo> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Map<String, HostGroupInfo> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public HostGroupInfo getHostGroup(String hostGroupName) {
        HostGroupInfo hg = hostGroups.get(hostGroupName);
        if (hg == null) {
            throw new IllegalArgumentException("could not find host group " + hostGroupName);
        }
        return hg;
    }

    public List<String> descending(Map<String, EnvironmentInfo> infoMap) {
        return Inherits.descending(name, e -> infoMap.get(e).parents.iterator());
    }

    public List<String> descendingParents(Map<String, EnvironmentInfo> infoMap) {
        return Inherits.descendingParents(name, e -> infoMap.get(e).parents.iterator());
    }

    public LayeredVariables getFullVars(Map<String, EnvironmentInfo> envs) {
        LayeredVariables vars = new LayeredVariables();
        for (String e : descending(envs)) {
            vars.layer(envs.get(e).getVars());
        }
        return vars;
    }
}
