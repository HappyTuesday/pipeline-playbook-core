package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.Project;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface ProjectDSL extends ProjectOverrideDSL {

    default ProjectDSL closure(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = KeyedProjectDSL.class) Closure closure) {
        Closures.delegateOnly(this, closure);
        return this;
    }

    default void desc(String description) {
        getProject().setDescription(description);
    }

    ProjectDSL activeInEnv(String envName);

    default ProjectDSL abstracted() {
        getProject().setAbstracted(true);
        return this;
    }

    /**
     * @deprecated use abstractProject instead
     */
    @Deprecated
    default ProjectDSL abstracted(boolean abstracted) {
        getProject().setAbstracted(abstracted);
        return this;
    }

    ProjectDSL inherits(String projectName);

    /**
     * project name generator for its children
     * will be evaluated by a key parameter when before creating a child project
     * the key will be the key of the created child project
     */
    default ProjectDSL projectNameGenerator(@ClosureParams(value = SimpleType.class, options = {"java.lang.String"}) Closure<String> closure) {
        getProject().setProjectNameGenerator(new ClosureWrapper<>(closure));
        return this;
    }

    /**
     * project variable group generator
     * will be evaluated by a key parameter when before creating a child project
     * the key will be the key of the created child project
     */
    default ProjectDSL variableGroup(@ClosureParams(value = SimpleType.class, options = {"com.yit.deploy.core.model.Project"}) Closure<String> closure) {
        getProject().setVariableGroupGenerator(new ClosureWrapper<>(closure));
        return this;
    }

    ProjectDSL playbook(String playbookName);

    default ProjectDSL when(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = JobEvaluationContext.class) Closure<Boolean> closure) {
        if (getProject().getWhen() == null) {
            getProject().setWhen(new ArrayList<>());
        }
        getProject().getWhen().add(new ClosureWrapper<>(closure));
        return this;
    }

    /**
     * indicate that this project only be included in the environments
     * which have a name or labels or env-type or super env name included in the env parameter
     *
     * @param env env name/label list
     * @return this
     */
    default ProjectDSL includeOnlyEnv(String... env) {
        getProject().setIncludedOnlyInEnv(Arrays.asList(env));
        return this;
    }

    /**
     * indicate that this project only be included in the environments
     * which have a name or labels or env-type or super env name included in the env parameter
     *
     * @param env env name/label list
     * @return this
     */
    default ProjectDSL includeOnlyEnv(List<String> env) {
        getProject().setIncludedOnlyInEnv(env);
        return this;
    }

    /**
     * indicate that this project be included in the environments
     * which have a name or labels or env-type or super env name included in the env parameter
     *
     * @param env env name/label list
     * @return this
     */
    default ProjectDSL includeEnv(String... env) {
        getProject().setIncludedInEnv(Arrays.asList(env));
        return this;
    }

    /**
     * indicate that this project be included in the environments
     * which have a name or labels or env-type or super env name included in the env parameter
     *
     * @param env env name/label list
     * @return this
     */
    default ProjectDSL includeEnv(List<String> env) {
        getProject().setIncludedInEnv(env);
        return this;
    }

    /**
     * indicate that this project will not be included in the environments
     * which have a name or labels or env-type or super env name included in the env parameter
     *
     * @param env env name/label list
     * @return this
     */
    default ProjectDSL excludeEnv(String... env) {
        getProject().setExcludedInEnv(Arrays.asList(env));
        return this;
    }

    /**
     * indicate that this project will not be included in the environments
     * which have a name or labels or env-type or super env name included in the env parameter
     *
     * @param env env name/label list
     * @return this
     */
    default ProjectDSL excludeEnv(List<String> env) {
        getProject().setExcludedInEnv(env);
        return this;
    }

    SharingContext share(String fromEnv);

    interface SharingContext {
        SharingContext to(String... env);
    }
}
