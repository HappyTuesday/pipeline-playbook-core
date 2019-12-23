package com.yit.deploy.core.variables.resolvers;

import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.model.Project;
import com.yit.deploy.core.variables.CacheProvider;
import com.yit.deploy.core.variables.VariableAuthorizer;
import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.variable.Variable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

/**
 * context used when resolving a variable
 */
public class ResolveContext {
    /**
     * the target object against which to execute closures
     */
    public final Object target;
    /**
     * current environment
     */
    public final Environment env;
    /**
     * current project
     */
    public final Project project;
    /**
     * the user parameters
     */
    public final Map<String, Object> userParameters;
    /**
     * from where this variable is defined
     */
    public final Variables where;

    /**
     * used by those cached variable to save their values
     */
    public final CacheProvider cacheProvider;

    /**
     * used to check if you have the permission to visit the variable
     */
    public final VariableAuthorizer authorizer;

    public ResolveContext(Object target, Variables where, CacheProvider cacheProvider) {
        this(target, null, null, Collections.emptyMap(), where, cacheProvider, null);
    }

    private ResolveContext(Object target,
                           @Nullable Environment env,
                           @Nullable Project project,
                           Map<String, Object> userParameters,
                           Variables where,
                           CacheProvider cacheProvider,
                           VariableAuthorizer authorizer) {

        this.target = target;
        this.where = where;
        this.env = env;
        this.project = project;
        this.userParameters = userParameters;
        this.cacheProvider = cacheProvider;
        this.authorizer = authorizer;
    }

    /**
     * change to new where
     * @param where against which variable table the variable resolving occurred
     * @return new resolve context instance
     */
    public ResolveContext where(Variables where) {
        return new ResolveContext(target, env, project, userParameters, where, cacheProvider, authorizer);
    }

    public ResolveContext userParameters(Map<String, Object> userParameters) {
        return new ResolveContext(target, env, project, userParameters, where, cacheProvider, authorizer);
    }

    public ResolveContext project(Project project) {
        return new ResolveContext(target, env, project, userParameters, where, cacheProvider, authorizer);
    }

    public ResolveContext env(Environment env) {
        return new ResolveContext(target, env, project, userParameters, where, cacheProvider, authorizer);
    }

    public ResolveContext cacheProvider(CacheProvider cacheProvider) {
        return new ResolveContext(target, env, project, userParameters, where, cacheProvider, authorizer);
    }

    public ResolveContext authorizing(VariableAuthorizer authorizer) {
        return new ResolveContext(target, env, project, userParameters, where, cacheProvider, authorizer);
    }
}
