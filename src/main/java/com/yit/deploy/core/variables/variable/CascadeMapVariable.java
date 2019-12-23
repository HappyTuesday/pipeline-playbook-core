package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.*;

public class CascadeMapVariable<T> extends MapVariable<T> {

    private final LinkedHashMap<String, Variable<T>> map;

    public CascadeMapVariable(@Nonnull VariableName name, String id, Iterable<Entry<String, Variable<T>>> inside, ResolveContext context) {
        super(name, id, context);
        this.map = new LinkedHashMap<>();
        for (Entry<String, Variable<T>> entry : inside) {
            this.map.put(entry.getKey(), entry.getValue().context(context).name(name.field(entry.getKey())));
        }
    }

    private CascadeMapVariable(CascadeMapVariable<T> that, ResolveContext context) {
        super(that.name, that.id, context);
        this.map = new LinkedHashMap<>(that.map.size());
        for (Entry<String, Variable<T>> entry : that.map.entrySet()) {
            this.map.put(entry.getKey(), entry.getValue().context(context));
        }
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
    public CascadeMapVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new CascadeMapVariable<>(name, id, this::variables, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public CascadeMapVariable<T> context(ResolveContext context) {
        if (Objects.equals(this.context, context)) {
            return this;
        }
        return new CascadeMapVariable<>(this, context);
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.cascadeMap, Lambda.mapValues(map, Variable::toInfo));
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
        return map.size();
    }

    /**
     * get the variable of the specified key in this map
     *
     * @param key key name
     * @return variable of the key
     */
    @Override
    public Variable<T> variable(String key) {
        for (Iterator<Variable<T>> iter = context.where.reverseFields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof ExpandableMapVariable) {
                Variable<T> vv = ((ExpandableMapVariable<T>) v).variable.context(context).variable(key);
                if (vv != null) {
                    return vv.name(name.field(key));
                }
            } else if (v.field().equals(key)) {
                return v.context(context);
            }
        }
        return map.get(key);
    }

    /**
     * get all variables defined in this map
     *
     * @return the iterator
     */
    @Nonnull
    @Override
    protected Iterator<Entry<String, Variable<T>>> variables() {
        Map<String, Variable<T>> result = new LinkedHashMap<>(this.map);
        for (Iterator<Variable<T>> iter = context.where.fields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof ExpandableMapVariable) {
                MapVariable<T> expanding = ((ExpandableMapVariable<T>) v).variable.context(context);
                for (Iterator<Entry<String, Variable<T>>> entries = expanding.variables(); entries.hasNext();) {
                    Entry<String, Variable<T>> entry = entries.next();
                    String key = entry.getKey();
                    // bind value to the specified name, since the variable comes from a non-cascade variable
                    result.put(key, entry.getValue().context(context).name(name.field(key)));
                }
            } else {
                result.put(v.field(), v.context(context));
            }
        }
        return result.entrySet().iterator();
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
        Set<String> close = new HashSet<>(map.keySet());
        for (Iterator<Variable<T>> iter = context.where.reverseFields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof ExpandableMapVariable) {
                close.addAll(((ExpandableMapVariable<T>) v).variable.context(context).keySet());
            } else {
                close.add(v.field());
            }
        }
        return close.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        if (!map.isEmpty()) {
            return false;
        }
        for (Iterator<Variable<T>> iter = context.where.reverseFields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof ExpandableMapVariable) {
                if (!((ExpandableMapVariable<T>) v).variable.context(context).isEmpty()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
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
        if (map.containsKey(key)) {
            return true;
        }
        for (Iterator<Variable<T>> iter = context.where.reverseFields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof ExpandableMapVariable) {
                return ((ExpandableMapVariable<T>) v).variable.context(context).containsKey(key);
            } else if (v.field().equals(key)) {
                return true;
            }
        }
        return false;
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
        context.where.put(Variable.toVariable(value).context(context).name(name.field(key)).context(null));
        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation).  The effect of this call is equivalent to that
     * of calling {@link #put(Object, Object) put(k, v)} on this map once
     * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
     * specified map.  The behavior of this operation is undefined if the
     * specified map is modified while the operation is in progress.
     *
     * @param m mappings to be stored in this map
     * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
     *                                       is not supported by this map
     * @throws ClassCastException            if the class of a key or value in the
     *                                       specified map prevents it from being stored in this map
     * @throws NullPointerException          if the specified map is null, or if
     *                                       this map does not permit null keys or values, and the
     *                                       specified map contains null keys or values
     * @throws IllegalArgumentException      if some property of a key or value in
     *                                       the specified map prevents it from being stored in this map
     */
    @Override
    public void putAll(Map<? extends String, ? extends T> m) {
        Object value = Variable.unwrapContext(m);
        if (value instanceof MapVariable) {
            MapVariable<T> mv = (MapVariable<T>) value;

            if (mv.lazyMode()) {
                context.where.put(new ExpandableMapVariable<>(mv, name.toRepeatable(), mv.id));
            } else {
                for (Iterator<Entry<String, Variable<T>>> iter = mv.context(context).variables(); iter.hasNext(); ) {
                    Entry<String, Variable<T>> entry = iter.next();
                    put(entry.getKey(), (T) entry.getValue());
                }
            }

            return;
        }

        for (Entry<? extends String, ? extends T> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
