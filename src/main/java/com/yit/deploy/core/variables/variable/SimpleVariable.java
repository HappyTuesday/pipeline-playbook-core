package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import groovy.lang.GString;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Created by nick on 31/08/2017.
 */
public class SimpleVariable<T> extends BaseVariable<T> {

    private T value;

    public SimpleVariable(T value) {
        this(value, null, null);
    }

    public SimpleVariable(T value, VariableName name, String id) {
        super(name, id);
        this.value = value;
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
    public SimpleVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new SimpleVariable<>(value, name, id);
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.simple, value);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public T get(ResolveContext context) {
        Object value = this.value;
        if (value == null) {
            return null;
        }
        if (value instanceof GString) {
            value = value.toString();
        }
        return (T) value;
    }

    public String toString() {
        return value != null ? value.toString() : null;
    }
}
