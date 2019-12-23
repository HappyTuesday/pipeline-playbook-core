package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.model.VariableName;

public abstract class BaseVariable<T> implements Variable<T> {

    protected final VariableName name;
    protected final String id;

    protected BaseVariable(VariableName name, String id) {
        this.id = id;
        this.name = name;
    }

    /**
     * the name of the variable
     *
     * @return variable name
     */
    @Override
    public VariableName name() {
        return name;
    }

    /**
     * get the id of the variable
     *
     * @return id
     */
    @Override
    public String id() {
        return id;
    }
}
