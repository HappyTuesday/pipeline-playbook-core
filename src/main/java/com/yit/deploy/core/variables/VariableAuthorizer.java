package com.yit.deploy.core.variables;

import com.yit.deploy.core.variables.variable.Variable;

@FunctionalInterface
public interface VariableAuthorizer {
    boolean authorize(Variable variable);
}
