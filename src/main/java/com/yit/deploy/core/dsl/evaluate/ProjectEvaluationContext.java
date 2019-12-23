package com.yit.deploy.core.dsl.evaluate;

import com.yit.deploy.core.dsl.support.PlaybookParametersVarSupport;
import com.yit.deploy.core.dsl.support.ProjectEvaluationSupport;
import com.yit.deploy.core.exceptions.InvalidVariableDefinitionException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import com.yit.deploy.core.variables.resolvers.VariableResolving;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ProjectEvaluationContext extends EnvironmentEvaluationContext implements PlaybookParametersVarSupport, ProjectEvaluationSupport {

    private final Project project;
    private final Variables projectVars;

    public ProjectEvaluationContext(Project project, Environment env, DeployModelTable modelTable) {
        super(env, modelTable);
        this.project = project;
        this.projectVars = this.project.getVarsForEnv(env);
        resolveVars(this.projectVars);
    }

    @Override
    public ProjectEvaluationContext getExecutionContext() {
        return this;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public Variables getProjectVars() {
        return projectVars;
    }

    /**
     * get the resolve context
     *
     * @return resolve context
     */
    @Override
    public ResolveContext getResolveContext() {
        return super.getResolveContext().project(project);
    }

    @Override
    public ProjectEvaluationSupport withProject(Project project, Environment env) {
        Job job = modelTable.findJobWithSharing(project.getProjectName(), env.getName());

        if (job == null) {
            return new ProjectEvaluationContext(project, env, modelTable);
        }

        return new JobEvaluationContext(job);
    }
}
