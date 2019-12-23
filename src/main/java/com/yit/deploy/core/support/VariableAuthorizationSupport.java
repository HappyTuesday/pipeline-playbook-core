package com.yit.deploy.core.support;

import com.yit.deploy.core.dsl.evaluate.EvaluationContext;
import com.yit.deploy.core.variables.variable.EncryptedVariable;
import groovy.lang.Closure;

import java.util.Collection;

public interface VariableAuthorizationSupport {
    EvaluationContext getExecutionContext();

    default void denyEncryptedVars(Closure closure) {
        getExecutionContext().withAuthorization(
            v -> !(v instanceof EncryptedVariable)
        ).delegateOnly(closure);
    }

    default void variableWhitelist(Collection<String> allowedVars, Closure closure) {
        getExecutionContext().withAuthorization(
            v -> allowedVars.contains(v.name().toString())
        ).delegateOnly(closure);
    }
}
