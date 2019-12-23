package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.exceptions.AbstractedVariableException;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import groovy.lang.Closure;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ClosureVariable<T> extends BaseVariable<Closure<T>> {

    private ClosureWrapper<T> closure;

    public ClosureVariable(ClosureWrapper<T> closure) {
        this(closure, null, null);
    }

    public ClosureVariable(ClosureWrapper<T> closure, VariableName name, String id) {
        super(name, id);
        this.closure = closure;
    }

    /**
     * create a new variable instance with a different name
     * <p>
     * NOTE: name field must be readonly, any change to the name field of a variable will create a new instance
     *
     * @param name the new variable name
     * @return new created variable
     */
    @Override
    public ClosureVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new ClosureVariable<>(closure, name, id);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public Closure<T> get(ResolveContext context) {
        if (closure == null) {
            throw new AbstractedVariableException(name);
        }
        return closure.getClosure();
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.closure, closure == null ? null : closure.toInfo());
    }
}
