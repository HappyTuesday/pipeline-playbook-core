package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CachedVariable<T> extends BaseVariable<T> {

    private Variable<T> backend;

    public CachedVariable(Variable<T> backend) {
        this(backend, null, null);
    }

    public CachedVariable(Variable<T> backend, VariableName name, String id) {
        super(name, id);
        this.backend = backend.context(null);
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
    public Variable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new CachedVariable<>(backend, name, id);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public T get(ResolveContext context) {
        if (context.cacheProvider == null || this.name == null) {
            return backend.resolve(context);
        }
        return context.cacheProvider.withCache(this.name.toString(), key -> backend.resolve(context));
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.cached, backend.toInfo());
    }
}
