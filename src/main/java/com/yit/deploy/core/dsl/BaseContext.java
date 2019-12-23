package com.yit.deploy.core.dsl;

import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.support.CodeRegionSupport;
import com.yit.deploy.core.variables.resolvers.SimpleVariableResolver;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.support.VariableValueTypesSupport;
import groovy.lang.Closure;

public abstract class BaseContext extends SimpleVariableResolver implements VariableValueTypesSupport, CodeRegionSupport {
    public VariableResolver getExecutionContext() {
        return this;
    }

    public void delegateOnly(Closure closure) {
        //noinspection unchecked
        Closures.withDelegateOnly(this, closure);
    }
}
