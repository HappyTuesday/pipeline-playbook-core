package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.variables.variable.CachedVariable;
import com.yit.deploy.core.variables.variable.Variable;

/**
 * Created by nick on 31/08/2017.
 */
public class VariableContext<T> extends BaseContext {
    public Variable<T> variable;

    public VariableContext(Variable<T> var) {
        this.variable = var;
    }

    public VariableContext<T> enableCache() {
        variable = new CachedVariable<>(variable);
        return this;
    }

    public UserParameterContext<T> parameter() {
        return parameter("string");
    }

    public UserParameterContext<T> parameter(String type) {
        return new UserParameterContext<>(variable, type);
    }
}
