package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

/**
 * a contextual variable, when resolving, it will return itself as the resolve result but including some context info
 * @param <T> the target value type of which we want to play
 * @param <V> the variable type, commonly set to the inheriting class type
 */
public abstract class ContextualVariable<T, V extends ContextualVariable<T, V>> extends BaseVariable<V> implements Variable<V> {

    /**
     * current resolve context
     */
    protected final ResolveContext context;

    public ContextualVariable(VariableName name, String id, ResolveContext context) {
        super(name, id);
        this.context = context;
    }

    /**
     * prepare for save
     *
     * @param name the name of the variable to save
     * @return the version of this variable to save
     */
    @Override
    public Variable<V> saving(VariableName name) {
        Variable<V> v = this.name(name);
        if (v instanceof ContextualVariable && this.context != null) {
            v = v.context(null);
        }
        return v;
    }

    /**
     * get the resolve context saved before
     *
     * @return resolve context
     */
    public ResolveContext context() {
        return context;
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public abstract V context(ResolveContext context);

    /**
     * resolve and return a variable-free object
     *
     * @return value
     */
    public Object concrete() {
        return concrete(context);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public V get(ResolveContext context) {
        // name and context must be learned when preparing in sub class.
        // and if the name passed here does not equals to the name when preparing, the name learned when preparing wins
        return context(context);
    }
}
