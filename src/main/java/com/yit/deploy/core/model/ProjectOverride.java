package com.yit.deploy.core.model;

import com.yit.deploy.core.variables.SimpleVariables;

public class ProjectOverride {
    private final EnvironmentQuery query;
    private final SimpleVariables vars;

    public ProjectOverride(EnvironmentQuery query, SimpleVariables vars) {
        this.query = query;
        this.vars = vars;
    }

    public EnvironmentQuery getQuery() {
        return query;
    }

    public SimpleVariables getVars() {
        return vars;
    }
}
