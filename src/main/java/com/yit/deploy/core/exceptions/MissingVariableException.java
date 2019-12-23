package com.yit.deploy.core.exceptions;

import com.yit.deploy.core.model.VariableName;

/**
 * Created by nick on 12/09/2017.
 */
public class MissingVariableException extends ResolveVariableException {
    public MissingVariableException(String variableName) {
        super(variableName, "variable " + variableName + " is missing");
    }

    public MissingVariableException(VariableName variableName) {
        this(variableName.toString());
    }
}
