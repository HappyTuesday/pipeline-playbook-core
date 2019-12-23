package com.yit.deploy.core.records;

import com.yit.deploy.core.info.ClosureInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectRecord implements Record {
    private long commitId;
    private String projectName;
    private ClosureInfo projectNameGenerator;
    private ClosureInfo variableGroupGenerator;
    private String key;
    private String activeInEnv;
    private String description;
    private List<String> parents;
    private Boolean abstracted;
    private boolean disabled;
    private String playbookName;
    private List<ClosureInfo> when;
    private List<String> includedInEnv;
    private List<String> includedOnlyInEnv;
    private List<String> excludedInEnv;
    private LinkedHashMap<String, String> sharing;

    public long getCommitId() {
        return commitId;
    }

    public void setCommitId(long commitId) {
        this.commitId = commitId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public ClosureInfo getProjectNameGenerator() {
        return projectNameGenerator;
    }

    public void setProjectNameGenerator(ClosureInfo projectNameGenerator) {
        this.projectNameGenerator = projectNameGenerator;
    }

    public ClosureInfo getVariableGroupGenerator() {
        return variableGroupGenerator;
    }

    public void setVariableGroupGenerator(ClosureInfo variableGroupGenerator) {
        this.variableGroupGenerator = variableGroupGenerator;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return projectName;
    }

    public String getActiveInEnv() {
        return activeInEnv;
    }

    public void setActiveInEnv(String activeInEnv) {
        this.activeInEnv = activeInEnv;
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

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    public Boolean getAbstracted() {
        return abstracted;
    }

    public void setAbstracted(Boolean abstracted) {
        this.abstracted = abstracted;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getPlaybookName() {
        return playbookName;
    }

    public void setPlaybookName(String playbookName) {
        this.playbookName = playbookName;
    }

    public List<ClosureInfo> getWhen() {
        return when;
    }

    public void setWhen(List<ClosureInfo> when) {
        this.when = when;
    }

    public List<String> getIncludedInEnv() {
        return includedInEnv;
    }

    public void setIncludedInEnv(List<String> includedInEnv) {
        this.includedInEnv = includedInEnv;
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

    public LinkedHashMap<String, String> getSharing() {
        return sharing;
    }

    public void setSharing(LinkedHashMap<String, String> sharing) {
        this.sharing = sharing;
    }
}
