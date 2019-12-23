package com.yit.deploy.core.info;

import java.util.List;
import java.util.Map;

public class ProjectPlaybookInfo {
    private String playbookName;
    private Map<String, Object> playbookParams;
    public List<ProjectPlaybookOverrideInfo> overrides;

    public String getPlaybookName() {
        return playbookName;
    }

    public void setPlaybookName(String playbookName) {
        this.playbookName = playbookName;
    }

    public Map<String, Object> getPlaybookParams() {
        return playbookParams;
    }

    public void setPlaybookParams(Map<String, Object> playbookParams) {
        this.playbookParams = playbookParams;
    }

    public List<ProjectPlaybookOverrideInfo> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<ProjectPlaybookOverrideInfo> overrides) {
        this.overrides = overrides;
    }
}
