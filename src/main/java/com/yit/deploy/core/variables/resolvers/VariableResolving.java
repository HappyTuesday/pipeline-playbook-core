package com.yit.deploy.core.variables.resolvers;

import com.yit.deploy.core.variables.variable.AbstractVariable;
import com.yit.deploy.core.variables.variable.ContextualVariable;
import com.yit.deploy.core.variables.variable.Variable;

import java.util.List;
import java.util.Map;

public class VariableResolving<T> {
    private final Variable<T> variable;
    private final ResolveContext context;

    public VariableResolving(Variable<T> variable, ResolveContext context) {
        this.variable = variable;
        this.context = context;
    }

    /**
     * resolve the variable
     * @return variable value
     */
    public T resolve() {
        return variable.resolve(context);
    }

    public void append(Object value) {
        Object object = resolve();
        if (object instanceof List) {
            //noinspection unchecked
            ((List<Object>) object).add(value);
        } else if (object instanceof Map) {
            //noinspection unchecked
            ((Map<Object, Object>) object).putAll((Map) value);
        } else {
            throw new IllegalArgumentException("variable " + variable.name() + " is not list nor map var");
        }
    }

    public Object concrete() {
        return variable.concrete(context);
    }

    public boolean isAbstracted() {
        return variable instanceof AbstractVariable;
    }
}
