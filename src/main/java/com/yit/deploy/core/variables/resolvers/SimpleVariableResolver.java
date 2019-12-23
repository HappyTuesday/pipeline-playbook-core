package com.yit.deploy.core.variables.resolvers;

import com.yit.deploy.core.dsl.support.MemoryCacheProvider;
import com.yit.deploy.core.exceptions.AbstractedVariableException;
import com.yit.deploy.core.exceptions.MissingVariableException;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.CacheProvider;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.variable.Variable;
import com.yit.deploy.core.variables.Variables;

import javax.annotation.Nullable;
import java.util.*;

public class SimpleVariableResolver implements VariableResolver {

    /**
     * it is private layers, please do not modify it directly
     */
    private final LayeredVariables vars = new LayeredVariables();
    private final CacheProvider defaultCacheProvider = new MemoryCacheProvider();

    public SimpleVariableResolver() {
    }

    public SimpleVariableResolver(Variables layer) {
        vars.layer(layer);
    }

    /**
     * return the underline vars
     *
     * @return the variable table
     */
    @Override
    public Variables getUnderlineVars() {
        return vars;
    }

    public void setWritableVars(Variables writableLayer) {
        vars.setWritable(writableLayer);
    }

    public SimpleVariableResolver resolveVars(List<Variables> varsList) {
        vars.layer(varsList);
        return this;
    }

    /**
     * iterate all variables defined in this resolver
     *
     * @return variable iterator
     */
    @Override
    public Iterator<Variable> getVariables() {
        return vars.variables();
    }

    /**
     * get the resolve context
     *
     * @return resolve context
     */
    @Override
    public ResolveContext getResolveContext() {
        return new ResolveContext(this, vars, defaultCacheProvider);
    }

    /**
     * determine which variable used to resolve
     *
     * @param name variable name
     * @return variable instance
     */
    @Nullable
    public VariableResolving<?> determineVariable(String name) {
        return determineVariable(VariableName.parse(name));
    }

    /**
     * determine which variable used to resolve
     *
     * @param name variable name
     * @return variable instance
     */
    @Nullable
    public VariableResolving<?> determineVariable(VariableName name) {
        return determineVariableInVars(name);
    }

    /**
     * determine which variable used to resolve, in vars
     *
     * @param name variable name
     * @return variable instance
     */
    @Nullable
    public VariableResolving<?> determineVariableInVars(VariableName name) {
        Variable<?> var = vars.get(name);
        return var == null ? null : new VariableResolving<>(var, getResolveContext());
    }

    @Override
    public boolean variableExists(String name) {
        VariableResolving<?> resolving = determineVariable(name);
        return resolving != null && !resolving.isAbstracted();
    }

    /**
     * resolve the variable
     *
     * @param name    variable name
     * @return variable value
     */
    @Override
    public Object getVariable(String name) {
        VariableResolving<?> resolving = determineVariable(name);
        if (resolving == null) {
            throw new MissingVariableException(name);
        }
        return resolving.resolve();
    }

    /**
     * resolve the variable
     *
     * @param variable variable
     * @return variable value
     */
    @Override
    public <T> T getVariable(@Nullable Variable<T> variable) {
        if (variable == null) {
            return null;
        }
        return new VariableResolving<>(variable, getResolveContext()).resolve();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getVariableOrDefault(String name, T defaultValue) {
        VariableResolving<?> resolving = determineVariable(name);
        if (resolving == null) {
            return defaultValue;
        }

        try {
            return (T) resolving.resolve();
        } catch (MissingVariableException | AbstractedVariableException ignore) {
            return defaultValue;
        }
    }

    public <T> Object concreteVariable(@Nullable Variable<T> variable) {
        if (variable == null) {
            return null;
        }
        return new VariableResolving<>(variable, getResolveContext()).concrete();
    }

    public Object concreteVariable(String name) {
        VariableResolving<?> resolving = determineVariable(name);
        if (resolving == null) {
            throw new MissingVariableException(name);
        }
        return resolving.concrete();
    }

    @Override
    public <T> T concreteVariableOrDefault(String name, T defaultValue) {
        VariableResolving<?> resolving = determineVariable(name);
        if (resolving == null) {
            return defaultValue;
        }
        try {
            //noinspection unchecked
            return (T) resolving.concrete();
        } catch (MissingVariableException | AbstractedVariableException ignore) {
            return defaultValue;
        }
    }

    public <T> Variable<T> setVariable(String name, T value) {
        return vars.put(name, value);
    }

    @Override
    public void appendVariable(String name, Object value) {
        VariableResolving<?> resolving = determineVariable(name);
        if (resolving == null) {
            throw new MissingVariableException(name);
        }
        resolving.append(value);
    }
}
