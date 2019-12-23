package com.yit.deploy.core.variables;

import com.yit.deploy.core.algorithm.Merger;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import com.yit.deploy.core.variables.variable.Variable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * represent a set of variables
 */
public interface Variables extends Merger<Variables> {

    /**
     * check if this variable table is empty
     * @return return true if we are empty
     */
    boolean isEmpty();

    /**
     * check if the variable with given name is hidden by this variable table,
     * a variable is hidden only if there is a variable with the same name or its parent name
     *
     * @param name variable name
     * @return true if the name is hidden
     */
    boolean hidden(@Nonnull VariableName name);

    /**
     * get a variable by its name, can only get non-repeatable variables
     * including invisible variable
     *
     * @param name variable name
     * @return retrieved variable
     */
    @Nullable
    <T> Variable<T> getWithInvisible(@Nonnull VariableName name);

    /**
     * get a variable by its name, can only get non-repeatable variables
     *
     * @param name variable name
     * @return retrieved variable
     */
    @Nullable
    default <T> Variable<T> get(@Nonnull VariableName name) {
        Variable<T> v = getWithInvisible(name);
        return v == INVISIBLE ? null : v;
    }
    /**
     * get a variable by its name, can only get non-repeatable variables
     *
     * @param name variable name
     * @return retrieved variable
     */
    default <T> Variable<T> get(@Nonnull String name) {
        return get(VariableName.parse(name));
    }

    /**
     * get all fields defined under the variable of the name,
     * in the order at which the variables are put
     *
     * @param name variable name
     * @return an iterator iterating all fetched variables
     */
    @Nonnull
    <T> Iterator<Variable<T>> fields(@Nonnull VariableName name);

    /**
     * get all fields defined under the variable of the name,
     * in the reverse order at which the variables are put
     *
     * @param name variable name
     * @return an iterator iterating all fetched variables
     */
    @Nonnull
    <T> Iterator<Variable<T>> reverseFields(@Nonnull VariableName name);

    /**
     * iterate all variables, in the order at which the variables are put
     *
     * @return iterator
     */
    @Nonnull
    Iterator<Variable> variables();

    /**
     * iterate all variables (does not use hidden policy), in the order at which the variable are put
     * @return iterator
     */
    Iterator<Variable> allVariables();

    /**
     * get the writable variable table which will be used to put variable
     * @return writable variable table
     */
    @Nullable
    Variables getWritable();

    /**
     * put a variable
     *
     * @param variable the variable to put
     */
    <T> void put(@Nonnull Variable<T> variable);

    /**
     * put a variable into this variable table
     * @param name unparsed variable name
     * @param value value
     * @return the actually put variable
     */
    default <T> Variable<T> put(String name, T value) {
        Variable<T> v = Variable.toVariable(value).saving(VariableName.parse(name));
        put(v);
        return v;
    }

    /**
     * put variables into this variable table
     * @param map variables
     */
    default void putAll(Map<String, ?> map) {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * put all variables defined in the variable table
     * @param vars variable table
     */
    @Override
    default void merge(Variables vars) {
        for (Iterator<Variable> iter = vars.variables(); iter.hasNext(); ) {
            Variable var = iter.next();
            put(var);
        }
    }

    /**
     * clear all variables in this table
     */
    void clear();

    /**
     * convert the variable table to variable info list
     * @return variable info list
     */
    default List<VariableInfo> toInfo() {
        List<VariableInfo> list = new ArrayList<>();
        for (Iterator<Variable> iter = variables(); iter.hasNext();) {
            list.add(iter.next().toInfo());
        }
        return list;
    }

    Variable INVISIBLE = new Variable() {

        @Override
        public VariableName name() {
            throw new IllegalStateException();
        }

        @Override
        public Variable name(@Nonnull VariableName name) {
            throw new IllegalStateException();
        }
        /**
         * get the id of the variable
         *
         * @return id
         */
        @Override
        public String id() {
            throw new IllegalStateException();
        }

        /**
         * get the value of this variable, only for override by its implementations, not for outer world
         *
         * @param context resolving context
         * @return the value of the variable
         */
        @Override
        public Object get(ResolveContext context) {
            throw new IllegalStateException();
        }

        /**
         * convert variable to variable info
         *
         * @return variable info
         */
        @Override
        public VariableInfo toInfo() {
            throw new IllegalStateException();
        }
    };
}
