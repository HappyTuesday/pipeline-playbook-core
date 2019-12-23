package com.yit.deploy.core.exceptions;

import com.yit.deploy.core.variables.variable.Variable;

public class AccessDeniedVariableException extends ResolveVariableException {
    public AccessDeniedVariableException(Variable variable) {
        super(variable.name().toString(), "Access to variable " + variable.name().toString() + " is denied");
    }
}
