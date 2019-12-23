package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.exceptions.MissingVariableException;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.info.ProjectInfoAccessor;
import com.yit.deploy.core.model.Environments;
import com.yit.deploy.core.model.Playbooks;
import com.yit.deploy.core.variables.LayeredVariables;

public class ProjectContext extends BaseContext implements KeyedProjectDSL, ProjectDSLImpl {
    private final ProjectInfo project;
    private final LayeredVariables envVars;
    private final Environments envs;
    private final LayeredVariables playbookVars;
    private final Playbooks playbooks;
    private final LayeredVariables projectVars;
    private final ProjectInfoAccessor accessor;

    public ProjectContext(ProjectInfo project,
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

        String playbookName = project.getProjectName().replace('-', '_');
        if (playbooks.isValidPlaybook(playbookName)) {
            project.setPlaybookName(playbookName);
        }

        resolveVars(this.envVars, this.playbookVars, this.projectVars);

        refreshEnvVars();
        refreshProjectVars();

        setWritableVars(project.getVars());

        // depend on project vars, must be after refreshProjectVars()
        refreshPlaybook();
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

    @Override
    public String getKey() {
        String key = getProject().getKey();
        if (key == null) {
            throw new MissingVariableException("key");
        }
        return key;
    }
}