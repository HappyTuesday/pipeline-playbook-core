package com.yit.deploy.core.exceptions;

import com.yit.deploy.core.model.VariableName;

/**
 * Created by nick on 12/09/2017.
 */
public class AbstractedVariableException extends ResolveVariableException {
    public AbstractedVariableException(String variableName) {
        super(variableName, "variable " + variableName + " is abstract");
    }

    public AbstractedVariableException(VariableName variableName) {
        this(variableName.toString());
    }
}
