package com.yit.deploy.core.info;

import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.Task;

import java.util.*;

public class TaskInfo {
    private String path;
    private ClosureWrapper closure;
    private List<ClosureWrapper<Boolean>> when = new ArrayList<>();
    private List<String> includedOnlyInEnv;
    private List<String> excludedInEnv;
    private int retries;
    private List<String> tags = new ArrayList<>();
    private List<String> resourcesRequired = new ArrayList<>();
    private boolean includeRetiredHosts;
    private boolean onlyRetiredHosts; // only used if includeRetiredHosts is true
    private boolean reverse;

    public TaskInfo(String path) {
        this.path = path;
    }

    public TaskInfo(TaskInfo info) {
        this.path = info.path;
        this.closure = info.closure;
        this.when.addAll(info.when);
        if (info.includedOnlyInEnv != null) {
            this.includedOnlyInEnv = new ArrayList<>(info.includedOnlyInEnv);
        }
        if (info.excludedInEnv != null) {
            this.excludedInEnv = new ArrayList<>(info.excludedInEnv);
        }
        this.retries = info.retries;
        this.tags.addAll(info.tags);
        this.resourcesRequired.addAll(info.resourcesRequired);
        this.includeRetiredHosts = info.includeRetiredHosts;
        this.onlyRetiredHosts = info.onlyRetiredHosts;
        this.reverse = info.reverse;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ClosureWrapper getClosure() {
        return closure;
    }

    public void setClosure(ClosureWrapper closure) {
        this.closure = closure;
    }

    public List<ClosureWrapper<Boolean>> getWhen() {
        return when;
    }

    public void setWhen(List<ClosureWrapper<Boolean>> when) {
        this.when = when;
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

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getResourcesRequired() {
        return resourcesRequired;
    }

    public void setResourcesRequired(List<String> resourcesRequired) {
        this.resourcesRequired = resourcesRequired;
    }

    public boolean isIncludeRetiredHosts() {
        return includeRetiredHosts;
    }

    public void setIncludeRetiredHosts(boolean includeRetiredHosts) {
        this.includeRetiredHosts = includeRetiredHosts;
    }

    public boolean isOnlyRetiredHosts() {
        return onlyRetiredHosts;
    }

    public void setOnlyRetiredHosts(boolean onlyRetiredHosts) {
        this.onlyRetiredHosts = onlyRetiredHosts;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public String getFolder() {
        return getFolder(path);
    }

    public static String getFolder(String path) {
        int index = path.lastIndexOf(Task.PATH_SPLITTER);
        return index < 0 ? null : path.substring(0, index);
    }
}
