package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.support.PlaybookParametersVarSupport;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.model.EnvironmentQuery;
import com.yit.deploy.core.model.Project;
import com.yit.deploy.core.support.EncryptedVariableTypesSupport;
import com.yit.deploy.core.support.UserParameterValueTypesSupport;
import com.yit.deploy.core.support.VariableValueTypesSupport;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public interface ProjectOverrideDSL extends VariableResolver, PlaybookParametersVarSupport, VariableValueTypesSupport, EncryptedVariableTypesSupport, UserParameterValueTypesSupport {

    ProjectInfo getProject();

    void refreshPlaybook();

    @Override
    default void setPlaybookParam(String name, Object value) {
        String varName = Project.PLAYBOOK_PARAMETERS_VARIABLE;
        if (!variableExists(varName)) {
            setVariable(varName, new HashMap<>());
        }
        appendVariable(varName, Collections.singletonMap(name, value));
        refreshPlaybook();
    }

    default ProjectOverrideDSL jobName(Object jobName) {
        setVariable(Project.JOB_NAME_VARIABLE, jobName);
        return this;
    }

    default ProjectOverrideDSL group(Object groupName) {
        setVariable(Project.GROUP_NAME_VARIABLE, groupName);
        return this;
    }

    default ProjectOverrideDSL section(Object sectionName) {
        setVariable(Project.SECTION_NAME_VARIABLE, sectionName);
        return this;
    }

    default ProjectOverrideDSL jobOrder(Object jobOrder) {
        setVariable(Project.JOB_ORDER_VARIABLE, jobOrder);
        return this;
    }

    default ProjectOverrideDSL containerLabel(Object value) {
        appendVariable(Project.CONTAINER_LABELS_VARIABLE, value);
        return this;
    }

    default ProjectOverrideDSL containerLabels(List<?> containerLabels) {
        setVariable(Project.CONTAINER_LABELS_VARIABLE, containerLabels);
        return this;
    }

    default ProjectOverrideDSL dependencies(List<?> dependencies) {
        setVariable(Project.DEPENDENCY_VARIABLE, dependencies);
        return this;
    }

    default ProjectOverrideDSL depend(Object value) {
        appendVariable(Project.DEPENDENCY_VARIABLE, value);
        return this;
    }

    default ProjectOverrideDSL gitRepositoryUrl(Object url) {
        setVariable(Project.GIT_REPOSITORY_URL_VARIABLE, url);
        return this;
    }

    default ProjectOverrideDSL projectGitName(Object name) {
        setVariable(Project.PROJECT_GIT_NAME_VARIABLE, name);
        return this;
    }

    default ProjectOverrideDSL defaultBranchName(Object defaultBranchName) {
        setVariable(Project.DEFAULT_BRANCH_VARIABLE, defaultBranchName);
        return this;
    }

    /**
     * specify a cron table at which the project will be scheduled by Jenkins
     * @param value schedule
     * @return this
     */
    default ProjectOverrideDSL schedule(Object value){
        setVariable(Project.SCHEDULE_VARIABLE, value);
        return this;
    }

    @Deprecated
    default ProjectOverrideDSL tasksToSkip(List<?> tasksToSkip) {
        setVariable(Project.TASKS_TO_SKIP_VARIABLE, tasksToSkip);
        return this;
    }

    @Deprecated
    default ProjectOverrideDSL taskToSkip(Object taskToSkip) {
        appendVariable(Project.TASKS_TO_SKIP_VARIABLE, taskToSkip);
        return this;
    }

    default ProjectOverrideDSL skipTasks(List<?> tasksToSkip) {
        setVariable(Project.TASKS_TO_SKIP_VARIABLE, tasksToSkip);
        return this;
    }

    default ProjectOverrideDSL skipTask(Object taskToSkip) {
        appendVariable(Project.TASKS_TO_SKIP_VARIABLE, taskToSkip);
        return this;
    }

    default ProjectOverrideDSL requireTasks(List<?> tasks) {
        setVariable(Project.REQUIRED_TASKS_VARIABLE, tasks);
        return this;
    }

    default ProjectOverrideDSL requireTask(Object task) {
        appendVariable(Project.REQUIRED_TASKS_VARIABLE, task);
        return this;
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     * @param env1 first env name or envtype or labels or super env name
     * @param env2 second env name or envtype or labels or super env name
     * @param env3 third env name or envtype or labels or super env name
     * @param env4 fifth env name or envtype or labels or super env name
     * @param env5 fifth env name or envtype or labels or super env name
     * @param env6 fifth env name or envtype or labels or super env name
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    default ProjectOverrideDSL override(String env1, String env2, String env3, String env4, String env5, String env6,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ProjectOverrideDSL.class)
                                            Closure closure) {
        return override(new EnvironmentQuery(env1)
                .or(new EnvironmentQuery(env2))
                .or(new EnvironmentQuery(env3))
                .or(new EnvironmentQuery(env4))
                .or(new EnvironmentQuery(env5))
                .or(new EnvironmentQuery(env6))
            , closure);
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     * @param env1 first env name or envtype or labels or super env name
     * @param env2 second env name or envtype or labels or super env name
     * @param env3 third env name or envtype or labels or super env name
     * @param env4 fifth env name or envtype or labels or super env name
     * @param env5 fifth env name or envtype or labels or super env name
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    default ProjectOverrideDSL override(String env1, String env2, String env3, String env4, String env5,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ProjectOverrideDSL.class)
                                            Closure closure) {
        return override(new EnvironmentQuery(env1)
                .or(new EnvironmentQuery(env2))
                .or(new EnvironmentQuery(env3))
                .or(new EnvironmentQuery(env4))
                .or(new EnvironmentQuery(env5))
            , closure);
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     * @param env1 first env name or envtype or labels or super env name
     * @param env2 second env name or envtype or labels or super env name
     * @param env3 third env name or envtype or labels or super env name
     * @param env4 fifth env name or envtype or labels or super env name
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    default ProjectOverrideDSL override(String env1, String env2, String env3, String env4,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ProjectOverrideDSL.class)
                                            Closure closure) {
        return override(new EnvironmentQuery(env1)
                .or(new EnvironmentQuery(env2))
                .or(new EnvironmentQuery(env3))
                .or(new EnvironmentQuery(env4))
            , closure);
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     * @param env1 first env name or envtype or labels or super env name
     * @param env2 second env name or envtype or labels or super env name
     * @param env3 third env name or envtype or labels or super env name
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    default ProjectOverrideDSL override(String env1, String env2, String env3,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ProjectOverrideDSL.class)
                                            Closure closure) {
        return override(new EnvironmentQuery(env1)
                .or(new EnvironmentQuery(env2))
                .or(new EnvironmentQuery(env3)),
            closure);
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     * @param query1 first env name or envtype or labels or super env name
     * @param query2 second env name or envtype or labels or super env name
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    default ProjectOverrideDSL override(String query1, String query2,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ProjectOverrideDSL.class)
                                            Closure closure) {
        return override(new EnvironmentQuery(query1).or(new EnvironmentQuery(query2)), closure);
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     * @param query env query
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    default ProjectOverrideDSL override(String query,
                                        @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ProjectOverrideDSL.class)
                                            Closure closure) {
        return override(new EnvironmentQuery(query), closure);
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     * @param query env query
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    ProjectOverrideDSL override(EnvironmentQuery query,
                                @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ProjectOverrideDSL.class)
                                    Closure closure);
}
