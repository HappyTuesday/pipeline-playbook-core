package com.yit.deploy.core.dsl.evaluate;

import com.yit.deploy.core.dsl.support.PlaybookParametersVarSupport;
import com.yit.deploy.core.dsl.support.ProjectEvaluationSupport;
import com.yit.deploy.core.exceptions.InvalidVariableDefinitionException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.resolvers.VariableResolving;
import groovy.lang.Tuple2;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class JobEvaluationContext extends PlaybookEvaluationContext implements PlaybookParametersVarSupport, ProjectEvaluationSupport {

    private final Job job;
    private final Variables projectVars;

    public JobEvaluationContext(JobEvaluationContext cx) {
        this(cx.job, cx.getPlaybookWritable());
    }

    public JobEvaluationContext(Job job) {
        this(job, new SimpleVariables());
    }

    public JobEvaluationContext(Job job, PlaybookEvaluationContext cx) {
        this(job, cx.getPlaybookWritable());
    }

    public JobEvaluationContext(Job job, Variables playbookWritable) {
        super(job.getPlaybook(), playbookWritable, job.getEnv(), job.getModelTable());
        this.job = job;
        this.projectVars = job.getVars();
        resolveVars(this.projectVars);
    }

    @Override
    public JobEvaluationContext getExecutionContext() {
        return this;
    }

    public JobEvaluationContext withJob(Job job) {
        return new JobEvaluationContext(job);
    }

    @Override
    public ProjectEvaluationSupport withProject(Project project, Environment env) {
        Job job = modelTable.findJobWithSharing(project.getProjectName(), env.getName());

        if (job == null) {
            return new ProjectEvaluationContext(project, env, modelTable);
        }

        return withJob(job);
    }

    public Job getJob() {
        return job;
    }

    public Project getProject() {
        return job.getProject();
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
        return super.getResolveContext().project(job.getProject());
    }

    /**
     * determine which variable used to resolve
     *
     * @param name variable name
     * @return variable instance
     */
    @Nullable
    @Override
    public VariableResolving<?> determineVariable(VariableName name) {
        VariableResolving<?> inVars = determineVariableInVars(name);
        if (inVars != null) {
            return inVars;
        }
        return determineVariableInGroups(name);
    }
}
