package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.evaluate.PlaybookParameterEvaluationContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.info.ProjectInfoAccessor;
import com.yit.deploy.core.model.EnvironmentQuery;
import com.yit.deploy.core.model.Environments;
import com.yit.deploy.core.model.Playbooks;
import com.yit.deploy.core.model.Project;
import com.yit.deploy.core.variables.LayeredVariables;
import groovy.lang.Closure;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public interface ProjectDSLImpl extends ProjectDSL {

    LayeredVariables getProjectVars();
    LayeredVariables getEnvVars();
    Environments getEnvs();
    Playbooks getPlaybooks();
    LayeredVariables getPlaybookVars();
    ProjectInfoAccessor getAccessor();

    default void refreshProjectVars() {
        getProject().refreshProjectVars(getProjectVars(), getAccessor(), getEnvs());
    }

    @Override
    default ProjectDSL inherits(String projectName) {
        ProjectInfo inherited = getAccessor().getProject(projectName);

        if (inherited.isAbstracted() == null || !inherited.isAbstracted()) {
            throw new IllegalArgumentException("project " + projectName + " is not an abstracted project");
        }
        if (Lambda.any(getProject().getParents(), p -> p.equals(projectName))) {
            throw new IllegalArgumentException("project " + projectName + " is already inherited by " + getProject().getProjectName());
        }

        getProject().getParents().add(projectName);
        refreshProjectVars();
        refreshPlaybook();
        return this;
    }

    default void refreshEnvVars() {
        String envName = getProject().nearest(getAccessor(), ProjectInfo::getActiveInEnv);
        if (envName == null) {
            throw new IllegalConfigException("activeInEnv is not configured for project " + getProject().getProjectName());
        }
        getEnvVars().clearLayers().layer(getEnvs().get(envName).getVars());
    }

    @Override
    default ProjectDSL activeInEnv(String envName) {
        if (Objects.equals(getProject().getActiveInEnv(), envName)) {
            return this;
        }
        // check if env name exists
        getEnvs().get(envName);
        getProject().setActiveInEnv(envName);
        refreshEnvVars();
        refreshProjectVars();
        refreshPlaybook();

        return this;
    }

    @Override
    default ProjectDSL playbook(String playbookName) {
        if (Objects.equals(getProject().getPlaybookName(), playbookName)) {
            return this;
        }

        getProject().setPlaybookName(playbookName);
        refreshPlaybook();
        return this;
    }

    default void refreshPlaybook() {
        getProject().refreshPlaybookVars(
            getProject().getPlaybookParams(getProjectVars()),
            getPlaybookVars(),
            getPlaybooks(),
            getAccessor()
        );
    }

    default SharingContext share(String fromEnv) {
        if (getEnvs().get(fromEnv).isAbstracted()) {
            throw new IllegalConfigException("only non-abstracted can be shared, env " + fromEnv + " is abstracted");
        }
        return new SharingContextImpl(fromEnv, this);
    }

    class SharingContextImpl implements SharingContext {
        private String fromEnv;
        private ProjectDSLImpl dsl;

        private SharingContextImpl(String fromEnv, ProjectDSLImpl dsl) {
            this.fromEnv = fromEnv;
            this.dsl = dsl;
        }

        public ProjectDSL.SharingContext to(String... env) {
            for (String e : env) {
                if (this.fromEnv.equals(e)) {
                    throw new IllegalConfigException("env " + e + " is shared by itself");
                }
                this.dsl.getProject().getSharing().put(e, this.fromEnv);
            }
            return this;
        }
    }

    /**
     * when in these envs, the vars should be override as defined in the provided closure
     *
     * @param query env query
     * @param closure a closure providing all needed var definitions
     * @return this
     */
    @Override
    default ProjectOverrideDSL override(EnvironmentQuery query, Closure closure) {
        ProjectOverrideContext context = new ProjectOverrideContext(
            getProject(),
            query,
            getAccessor(),
            getPlaybooks(),
            getEnvVars(),
            getEnvs());

        Closures.delegateOnly(context, closure);
        return context;
    }
}
