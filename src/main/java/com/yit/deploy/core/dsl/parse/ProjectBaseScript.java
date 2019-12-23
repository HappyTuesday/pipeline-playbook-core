package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseScript;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.info.ProjectInfoAccessor;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.SimpleVariables;

import java.util.Map;

public abstract class ProjectBaseScript extends BaseScript implements ProjectDSLImpl {
    private ProjectInfo project;
    private LayeredVariables envVars;
    private Environments envs;
    private LayeredVariables playbookVars;
    private Playbooks playbooks;
    private LayeredVariables projectVars;
    private ProjectInfoAccessor accessor;

    public void parse(ProjectInfo project,
                      Environments envs,
                      Playbooks playbooks,
                      ProjectInfoAccessor accessor) {

        this.project = project;
        this.envVars = new LayeredVariables();
        this.envs = envs;
        this.playbookVars = new LayeredVariables();
        this.playbooks = playbooks;
        this.projectVars = new LayeredVariables();
        this.accessor = accessor;

        resolveVars(this.envVars, this.playbookVars, this.projectVars);

        refreshEnvVars();

        setWritableVars(project.getVars());
        refreshProjectVars();

        // depend on project vars, must be after refreshProjectVars()
        refreshPlaybook();

        run();
    }

    @Override
    public ProjectInfo getProject() {
        return project;
    }

    @Override
    public LayeredVariables getProjectVars() {
        return projectVars;
    }

    @Override
    public ProjectInfoAccessor getAccessor() {
        return accessor;
    }

    @Override
    public LayeredVariables getEnvVars() {
        return envVars;
    }

    @Override
    public Environments getEnvs() {
        return envs;
    }

    @Override
    public LayeredVariables getPlaybookVars() {
        return playbookVars;
    }

    @Override
    public Playbooks getPlaybooks() {
        return playbooks;
    }
}