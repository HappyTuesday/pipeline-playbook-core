package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import com.yit.deploy.core.variables.SimpleEntry;

import javax.annotation.Nonnull;
import java.util.*;

public class LazyMapVariable<T> extends MapVariable<T> {

    private final Variable<Map<String, T>> variable;

    public LazyMapVariable(Variable<Map<String, T>> variable) {
        this(variable, null, null);
    }

    public LazyMapVariable(Variable<Map<String, T>> variable, VariableName name, String id) {
        this(variable, name, id, null);
    }

    private LazyMapVariable(Variable<Map<String, T>> variable, VariableName name, String id, ResolveContext context) {
        super(name, id, context);
        this.variable = variable.context(context);
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
    public LazyMapVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new LazyMapVariable<>(variable, name, id, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public LazyMapVariable<T> context(ResolveContext context) {
        if (Objects.equals(this.context, context)) {
            return this;
        }
        return new LazyMapVariable<>(variable, name, id, context);
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.lazyMap, variable.toInfo());
    }

    private Map<String, T> map() {
        return variable.resolve(context);
    }

    /**
     * get the variable of the specified key in this map
     *
     * @param key key name
     * @return variable of the key
     */
    @Override
    public Variable<T> variable(String key) {
        Map<String, T> map = map();
        if (map instanceof MapVariable) {
            ((MapVariable) map).variable(key);
        }
        T value = map.get(key);
        if (value == null) {
            return null;
        }
        return Variable.toVariable(value);
    }

    /**
     * get all variables defined in this map
     *
     * @return the iterator
     */
    @Nonnull
    @Override
    protected Iterator<Entry<String, Variable<T>>> variables() {
        Map<String, T> map = map();

        if (map instanceof MapVariable) {
            return ((MapVariable<T>) map).variables();
        }

        return Lambda.map(
            map.entrySet().iterator(),
            entry -> new SimpleEntry<>(entry.getKey(), Variable.toVariable(entry.getValue())));
    }

    /**
     * if the map variables works in lazy mode
     *
     * @return return true if in lazy mode
     */
    @Override
    public boolean lazyMode() {
        return true;
    }

    /**
     * estimate the size of this map
     *
     * @return size
     */
    @Override
    protected int estimateSize() {
        return 0;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return map().isEmpty();
    }

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return map().size();
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key
     * @throws ClassCastException   if the key is of an inappropriate type for
     *                              this map
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *                              does not permit null keys
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public boolean containsKey(Object key) {
        return map().containsKey(key);
    }

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * the key, the old value is replaced by the specified value.  (A map
     * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
     * if {@link #containsKey(Object) m.containsKey(k)} would return
     * <tt>true</tt>.)
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     * <tt>null</tt> if there was no mapping for <tt>key</tt>.
     * (A <tt>null</tt> return can also indicate that the map
     * previously associated <tt>null</tt> with <tt>key</tt>,
     * if the implementation supports <tt>null</tt> values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation
     *                                       is not supported by this map
     * @throws ClassCastException            if the class of the specified key or value
     *                                       prevents it from being stored in this map
     * @throws NullPointerException          if the specified key or value is null
     *                                       and this map does not permit null keys or values
     * @throws IllegalArgumentException      if some property of the specified key
     *                                       or value prevents it from being stored in this map
     */
    @Override
    public T put(String key, T value) {
        throw new UnsupportedOperationException();
    }
}
