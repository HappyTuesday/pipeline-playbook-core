package com.yit.deploy.core.support;

import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Closures;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.function.Function;

public interface ClosuresSupport {

    default <RESULT> Closure<RESULT> variableClosure(Function<VariableResolver, RESULT> def) {
        return variableClosure(def, def);
    }

    default <RESULT> Closure<RESULT> variableClosure(Object owner, Function<VariableResolver, RESULT> def) {
        return Closures.delegateClosure(owner, def);
    }

    default <T> T executeClosure(@DelegatesTo(value = TaskExecutionContext.class, strategy = Closure.DELEGATE_FIRST) Closure<T> closure, Object ... args) {
        return Closures.with(this, closure, args);
    }
}
