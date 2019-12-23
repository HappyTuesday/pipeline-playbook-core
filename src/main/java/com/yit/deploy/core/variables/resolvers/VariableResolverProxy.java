package com.yit.deploy.core.variables.resolvers;

import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.variable.Variable;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * proxy all variable resolving calls to another variable context instance
 */
public interface VariableResolverProxy extends VariableResolver {

    VariableResolver getVariableResolver();

    /**
     * return the underline vars
     *
     * @return the variable table
     */
    @Override
    default Variables getUnderlineVars() {
        return getVariableResolver().getUnderlineVars();
    }

    default void setWritableVars(Variables writableLayer) {
        getVariableResolver().setWritableVars(writableLayer);
    }

    default VariableResolver resolveVars(List<Variables> varsList) {
        return getVariableResolver().resolveVars(varsList);
    }

    /**
     * iterate all variables defined in this resolver
     *
     * @return variable iterator
     */
    @Override
    default Iterator<Variable> getVariables() {
        return getVariableResolver().getVariables();
    }

    /**
     * get the resolve context
     *
     * @return resolve context
     */
    @Override
    default ResolveContext getResolveContext() {
        return getVariableResolver().getResolveContext();
    }

    @Override
    default boolean variableExists(String name) {
        return getVariableResolver().variableExists(name);
    }

    /**
     * resolve the variable
     *
     * @param name    variable name
     * @return variable value
     */
    @Override
    default Object getVariable(String name) {
        return getVariableResolver().getVariable(name);
    }

    /**
     * resolve the variable
     *
     * @param variable variable
     * @return variable value
     */
    @Override
    default <T> T getVariable(@Nullable Variable<T> variable) {
        return getVariableResolver().getVariable(variable);
    }

    @Override
    default <T> T getVariableOrDefault(String name, T defaultValue) {
        return getVariableResolver().getVariableOrDefault(name, defaultValue);
    }

    @Override
    default Object concreteVariable(String name) {
        return getVariableResolver().concreteVariable(name);
    }

    @Override
    default <T> Object concreteVariable(@Nullable Variable<T> variable) {
        return getVariableResolver().concreteVariable(variable);
    }

    @Override
    default <T> T concreteVariableOrDefault(String name, T defaultValue) {
        return getVariableResolver().concreteVariableOrDefault(name, defaultValue);
    }

    default <T> Variable<T> setVariable(String name, T value) {
        return getVariableResolver().setVariable(name, value);
    }

    @Override
    default void appendVariable(String name, Object value) {
        getVariableResolver().appendVariable(name, value);
    }
}
