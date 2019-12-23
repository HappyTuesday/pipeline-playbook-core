package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.ResolveVariableException;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import groovy.lang.GString;

import javax.annotation.Nonnull;
import java.util.Objects;

public class LazyVariable<T> extends BaseVariable<T> {

    private final ClosureWrapper<T> closure;

    public LazyVariable(ClosureWrapper<T> closure) {
        this(closure, null, null);
    }

    public LazyVariable(ClosureWrapper<T> closure, VariableName name, String id) {
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
    public LazyVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new LazyVariable<>(closure, name, id);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public T get(ResolveContext context) {
        try {
            Object value = closure.withDelegateOnly(context.target);
            if (value instanceof GString) {
                value = value.toString();
            }
            return (T) value;
        } catch (ResolveVariableException e) {
            if (name != null) {
                e.getChain().addFirst(name.toString());
            }
            throw e;
        } catch (ExitException e) {
            throw e;
        } catch (Exception e) {
            if (ExitException.belongsTo(e)) {
                throw e;
            }
            if (name == null) {
                throw e;
            } else {
                throw new ResolveVariableException(name.toString(), null, e);
            }
        }
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.lazy, closure.toInfo());
    }
}
