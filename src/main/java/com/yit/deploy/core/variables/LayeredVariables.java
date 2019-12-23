package com.yit.deploy.core.variables;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.variable.Variable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class LayeredVariables implements Variables {

    private final List<Variables> layers = new ArrayList<>();
    private Variables writable;

    /**
     * check if this variable table is empty
     *
     * @return return true if we are empty
     */
    @Override
    public boolean isEmpty() {
        if (layers.isEmpty()) {
            return true;
        }
        for (Variables l : layers) {
            if (!l.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * check if the variable with given name is hidden by this variable table,
     * a variable is hidden only if there is a variable with the same name or its parent name
     *
     * @param name    variable name
     * @return true if the name is hidden
     */
    @Override
    public boolean hidden(@Nonnull VariableName name) {
        for (Variables l : layers) {
            if (l.hidden(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * get a variable by its name, can only get non-repeatable variables
     * including invisible variable
     *
     * @param name    variable name
     * @return retrieved variable
     */
    @Nullable
    @Override
    public <T> Variable<T> getWithInvisible(@Nonnull VariableName name) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            Variable<T> v = layers.get(i).getWithInvisible(name);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * get all fields defined under the variable of the name,
     * in the order at which the variables are put
     *
     * @param name    variable name
     * @return an iterator iterating all fetched variables
     */
    @Nonnull
    @Override
    public <T> Iterator<Variable<T>> fields(@Nonnull VariableName name) {
        if (layers.isEmpty()) {
            return Lambda.emptyIterator();
        }
        if (layers.size() == 1) {
            return layers.get(0).fields(name);
        }

        return Lambda.iterate(new Supplier<Variable<T>>() {

            Iterator<Variable<T>> iter;
            int i = 0;

            @Override
            public Variable<T> get() {
                while (true) {
                    if (iter != null && iter.hasNext()) {
                        Variable<T> v = iter.next();
                        if (unhidden(v.name(), i)) { // since iter is calculated from i - 1
                            return v;
                        }
                    } else if (i < layers.size()) {
                        iter = layers.get(i++).fields(name);
                    } else {
                        return null;
                    }
                }
            }
        });
    }

    /**
     * get all fields defined under the variable of the name,
     * in the reverse order at which the variables are put
     *
     * @param name    variable name
     * @return an iterator iterating all fetched variables
     */
    @Nonnull
    @Override
    public <T> Iterator<Variable<T>> reverseFields(@Nonnull VariableName name) {
        if (layers.isEmpty()) {
            return Lambda.emptyIterator();
        }
        if (layers.size() == 1) {
            return layers.get(0).reverseFields(name);
        }

        return Lambda.iterate(new Supplier<Variable<T>>() {

            Iterator<Variable<T>> iter;
            int i = layers.size() - 1;

            @Override
            public Variable<T> get() {
                while (true) {
                    if (iter != null && iter.hasNext()) {
                        Variable<T> v = iter.next();
                        if (unhidden(v.name(), i + 2)) { // since iter is calculated from i + 1
                            return v;
                        }
                    } else if (i >= 0) {
                        iter = layers.get(i--).reverseFields(name);
                    } else {
                        return null;
                    }
                }
            }
        });
    }

    /**
     * iterate all variables, in the order at which the variables are put
     *
     * @return iterator
     */
    @Nonnull
    @Override
    public Iterator<Variable> variables() {
        if (layers.isEmpty()) {
            return Lambda.emptyIterator();
        }
        if (layers.size() == 1) {
            return layers.get(0).variables();
        }

        return Lambda.iterate(new Supplier<Variable>() {

            Iterator<Variable> iter;
            int i = 0;

            @Override
            public Variable get() {
                while (true) {
                    if (iter == null || !iter.hasNext()) {
                        if (i < layers.size()) {
                            iter = layers.get(i++).variables();
                        } else {
                            return null;
                        }
                    } else {
                        Variable v = iter.next();
                        if (unhidden(v.name(), i)) {
                            return v;
                        }
                    }
                }
            }
        });
    }

    /**
     * iterate all variables (does not use hidden policy), in the order at which the variable are put
     *
     * @return iterator
     */
    @Override
    public Iterator<Variable> allVariables() {
        if (layers.isEmpty()) {
            return Lambda.emptyIterator();
        }
        if (layers.size() == 1) {
            return layers.get(0).allVariables();
        }

        return Lambda.iterate(new Supplier<Variable>() {

            Iterator<Variable> iter;
            int i = 0;

            @Override
            public Variable get() {
                while (true) {
                    if (iter == null || !iter.hasNext()) {
                        if (i < layers.size()) {
                            iter = layers.get(i++).allVariables();
                        } else {
                            return null;
                        }
                    } else {
                        return iter.next();
                    }
                }
            }
        });
    }

    /**
     * add a list of layers to resolve layers
     * @param layers layers
     * @return this
     */
    public LayeredVariables layer(Variables... layers) {
        for (Variables l : layers) {
            layer(l);
        }
        return this;
    }

    /**
     * add a list of layers to resolve layers
     * @param layers layers
     * @return this
     */
    public LayeredVariables layer(Collection<Variables> layers) {
        for (Variables l : layers) {
            layer(l);
        }
        return this;
    }

    /**
     * add a new layer
     *
     * @param layer new layer
     * @return this
     */
    public LayeredVariables layer(Variables layer) {
        layers.add(layer);
        return this;
    }

    /**
     * clear all layers
     */
    public LayeredVariables clearLayers() {
        layers.clear();
        return this;
    }

    /**
     * get the writable variable table which will be used to put variable
     *
     * @return writable variable table
     */
    @Override
    public Variables getWritable() {
        return writable.getWritable();
    }
    /**
     * put a variable
     *
     * @param variable the variable to put
     */
    @Override
    public <T> void put(@Nonnull Variable<T> variable) {
        if (writable == null) {
            throw new UnsupportedOperationException("write operation is not support");
        }
        writable.put(variable);
    }

    /**
     * clear all variables in this table
     */
    @Override
    public void clear() {
        layers.clear();
        writable = null;
    }

    /**
     * set writable layer
     *
     * @param writable layer
     */
    public void setWritable(Variables writable) {
        this.writable = writable;
    }

    private boolean unhidden(@Nonnull VariableName name, int index) {
        for (int i = index; i < layers.size(); i++) {
            if (layers.get(i).hidden(name)) {
                return false;
            }
        }
        return true;
    }
}
