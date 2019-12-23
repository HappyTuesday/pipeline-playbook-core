package com.yit.deploy.core.records;

import java.util.List;

public class EnvironmentRecord implements Record {
    private long commitId;
    private String name;
    private Boolean abstracted;
    private boolean disabled;
    private String description;
    private List<String> parents;
    private List<String> labels;

    public long getCommitId() {
        return commitId;
    }

    public void setCommitId(long commitId) {
        this.commitId = commitId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAbstracted() {
        return abstracted;
    }

    public void setAbstracted(Boolean abstracted) {
        this.abstracted = abstracted;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
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

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
}
