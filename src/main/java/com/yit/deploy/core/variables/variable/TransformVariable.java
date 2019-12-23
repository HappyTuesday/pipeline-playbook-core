package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.Objects;

public class TransformVariable<S, T> extends BaseVariable<T> {

    private final Variable<S> variable;
    private final ClosureWrapper<T> closure;

    public TransformVariable(Variable<S> variable, ClosureWrapper<T> closure) {
        this(variable, closure, null, null);
    }

    public TransformVariable(Variable<S> variable, ClosureWrapper<T> closure, VariableName name, String id) {
        super(name, id);
        this.variable = variable;
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
    public TransformVariable<S, T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new TransformVariable<>(variable, closure, name, id);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public T get(ResolveContext context) {
        return closure.withDelegateOnly(context.target, variable.resolve(context));
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.transform, variable.toInfo(), closure.toInfo());
    }
}
