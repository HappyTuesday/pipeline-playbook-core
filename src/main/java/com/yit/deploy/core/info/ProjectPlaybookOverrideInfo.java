package com.yit.deploy.core.info;

import com.yit.deploy.core.model.EnvironmentQuery;

import java.util.Map;

public class ProjectPlaybookOverrideInfo {
    private final EnvironmentQuery query;
    private final Map<String, Object> playbookParams;

    public ProjectPlaybookOverrideInfo(EnvironmentQuery query, Map<String, Object> playbookParams) {
        this.query = query;
        this.playbookParams = playbookParams;
    }

    public EnvironmentQuery getQuery() {
        return query;
    }

    public Map<String, Object> getPlaybookParams() {
        return playbookParams;
    }
}
