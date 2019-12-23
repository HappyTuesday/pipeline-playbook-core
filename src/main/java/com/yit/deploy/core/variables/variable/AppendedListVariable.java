package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.Objects;

public class AppendedListVariable<T> extends BaseVariable<T> {

    public final Variable<T> variable;

    public AppendedListVariable(Variable<T> variable) {
        this(variable, null, null);
    }

    public AppendedListVariable(Variable<T> variable, VariableName name, String id) {
        super(name, id);
        this.variable = variable.context(null);
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
        return new AppendedListVariable<>(variable, name, id);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public T get(ResolveContext context) {
        throw new IllegalStateException("AppendedVariable is not intended to be resolved");
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.appendedList, variable.toInfo());
    }
}
