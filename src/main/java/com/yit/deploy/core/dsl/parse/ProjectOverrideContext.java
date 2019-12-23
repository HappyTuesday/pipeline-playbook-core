package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.dsl.evaluate.PlaybookParameterEvaluationContext;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.info.ProjectInfoAccessor;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;
import groovy.lang.Closure;

import java.util.Collections;
import java.util.Map;

public class ProjectOverrideContext extends BaseContext implements ProjectOverrideDSL {
    private final ProjectInfo project;
    private final LayeredVariables projectVars;
    private final ProjectInfoAccessor accessor;
    private final LayeredVariables playbookVars;
    private final Playbooks playbooks;
    private final Variables envVars;
    private final Environments envs;
    private final ProjectOverride override;

    public ProjectOverrideContext(ProjectInfo project,
                                  EnvironmentQuery query,
                                  ProjectInfoAccessor accessor,
                                  Playbooks playbooks,
                                  Variables envVars,
                                  Environments envs) {
        this.project = project;
        this.accessor = accessor;
        this.playbookVars = new LayeredVariables();
        this.playbooks = playbooks;
        this.envVars = envVars;
        this.envs = envs;

        this.override = project.getOrCreateOverride(query);

        this.projectVars = new LayeredVariables();
        resolveVars(envVars, this.playbookVars, this.projectVars);
        setWritableVars(override.getVars());
        project.refreshProjectVars(this.projectVars, accessor, query, envs);

        // depend on project vars, must be after refreshProjectVars()
        refreshPlaybook();
    }

    public ProjectInfo getProject() {
        return project;
    }

    @Override
    public void refreshPlaybook() {
        getProject().refreshPlaybookVars(
            getProject().getPlaybookParams(projectVars),
            playbookVars,
            playbooks,
            accessor
        );
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     *
     * @param query env query
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    @Override
    public ProjectOverrideDSL override(EnvironmentQuery query, Closure closure) {
        ProjectOverrideContext context = new ProjectOverrideContext(
            project,
            override.getQuery().and(query),
            accessor,
            playbooks,
            envVars,
            envs);

        Closures.delegateOnly(context, closure);
        return context;
    }
}
