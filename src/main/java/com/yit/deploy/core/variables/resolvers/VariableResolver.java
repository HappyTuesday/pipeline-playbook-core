package com.yit.deploy.core.variables.resolvers;

import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.variable.MapVariable;
import com.yit.deploy.core.variables.variable.Variable;

import javax.annotation.Nullable;
import java.util.*;

/**
 * provide a variable resolving service
 * with storing a set of maps, resolve a variable's value or set a value to a certain variable
 */
public interface VariableResolver {

    /**
     * return the underline vars
     * @return the variable table
     */
    Variables getUnderlineVars();

    VariableResolver resolveVars(List<Variables> varsList);

    void setWritableVars(Variables vars);

    default void resolveWritableVars(Variables vars) {
        resolveVars(vars);
        setWritableVars(vars);
    }

    default VariableResolver resolveVars(Variables vars) {
        return resolveVars(Collections.singletonList(vars));
    }

    default VariableResolver resolveVars(Variables... vars) {
        return resolveVars(Arrays.asList(vars));
    }

    /**
     * iterate all variables defined in this resolver
     * @return variable iterator
     */
    Iterator<Variable> getVariables();

    /**
     * get the resolve context to use
     * @return resolve context
     */
    ResolveContext getResolveContext();

    boolean variableExists(String name);

    <T> Variable<T> setVariable(String name, T value);

    void appendVariable(String name, Object value);

    /**
     * resolve the variable
     * @param name variable name
     * @return variable value
     */
    Object getVariable(String name);

    @SuppressWarnings("unchecked")
    default  <T> T getVariable(String name, Class<T> clazz) {
        return (T) getVariable(name);
    }

    /**
     * resolve the variable
     * @param variable variable
     * @return variable value
     */
    <T> T getVariable(@Nullable Variable<T> variable);

    /**
     * resolve the variable
     * @param variable variable
     * @return variable value
     */
    default <T> MapVariable<T> getVariable(MapVariable<T> variable) {
        return (MapVariable<T>) getVariable((Variable) variable);
    }

    default Object getVariableOrDefault(String name) {
        return getVariableOrDefault(name, null);
    }

    <T> T getVariableOrDefault(String name, T defaultValue);

    Object concreteVariable(String name);

    <T> Object concreteVariable(@Nullable Variable<T> variable);

    <T> T concreteVariableOrDefault(String name, T defaultValue);

    /**
     * read the value of the property from variable context if is not a real defined property.
     */
    default Object propertyMissing(String property) {
        return getVariable(property);
    }

    /**
     * write the property and its value into variable context if it is not a real defined property.
     */
    default void propertyMissing(String property, Object value) {
        setVariable(property, value);
    }
}
