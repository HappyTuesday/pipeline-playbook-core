package com.yit.deploy.core.info;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.inherits.Inherits;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.Play;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;

import java.util.*;

public class PlayInfo {
    private final String name;
    private String description;
    private final List<String> parents = new ArrayList<>();
    private final Variables vars = new SimpleVariables();
    private final List<TaskInfo> tasks = new ArrayList<>();
    private Double serial;
    private ClosureWrapper<Boolean> when;
    private List<String> includedOnlyInEnv;
    private List<String> excludedInEnv;
    private Integer retries;
    private Boolean alwaysRun;
    private final transient Map<String, ResourceOperatorInfo> resourceOperators = new HashMap<>();
    private final List<String> resourcesRequired = new ArrayList<>();

    public PlayInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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

    public Variables getVars() {
        return vars;
    }

    public List<TaskInfo> getTasks() {
        return tasks;
    }

    public Double getSerial() {
        return serial;
    }

    public void setSerial(Double serial) {
        this.serial = serial;
    }

    public ClosureWrapper<Boolean> getWhen() {
        return when;
    }

    public void setWhen(ClosureWrapper<Boolean> when) {
        this.when = when;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Boolean isAlwaysRun() {
        return alwaysRun;
    }

    public void setAlwaysRun(Boolean alwaysRun) {
        this.alwaysRun = alwaysRun;
    }

    /**
     * resource operators defined for tasks
     */
    public Map<String, ResourceOperatorInfo> getResourceOperators() {
        return resourceOperators;
    }

    /**
     * resources that will be acquired before play execution
     */
    public List<String> getResourcesRequired() {
        return resourcesRequired;
    }

    public List<String> getIncludedOnlyInEnv() {
        return includedOnlyInEnv;
    }

    public void setIncludedOnlyInEnv(List<String> includedOnlyInEnv) {
        this.includedOnlyInEnv = includedOnlyInEnv;
    }

    public List<String> getExcludedInEnv() {
        return excludedInEnv;
    }

    public void setExcludedInEnv(List<String> excludedInEnv) {
        this.excludedInEnv = excludedInEnv;
    }

    public List<PlayInfo> descending(List<PlaybookInfo> playbookAscending) {
        return Inherits.descending(this, p -> Lambda.map(p.getParents().iterator(), s -> {
            PlayInfo pi;
            if (Play.INHERITS_SUPER.equals(s)) {
                pi = PlaybookInfo.findSuperPlay(p, playbookAscending);
            } else {
                pi = PlaybookInfo.findPlay(s, playbookAscending);
            }
            if (pi == null) {
                throw new IllegalArgumentException("could not find play " + s);
            }
            return pi;
        }));
    }
}
