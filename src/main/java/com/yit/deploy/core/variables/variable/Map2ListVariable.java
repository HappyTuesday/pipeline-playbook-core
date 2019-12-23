package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.*;

public class Map2ListVariable<T> extends ListVariable<Map.Entry<String, T>> {

    private final MapVariable<T> variable;

    public Map2ListVariable(MapVariable<T> variable) {
        this(variable, null, null);
    }

    public Map2ListVariable(MapVariable<T> variable, VariableName name, String id) {
        this(variable, name, id, null);
    }

    private Map2ListVariable(MapVariable<T> variable, VariableName name, String id, ResolveContext context) {
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
    public Map2ListVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new Map2ListVariable<>(variable, name, id, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public Map2ListVariable<T> context(ResolveContext context) {
        if (Objects.equals(this.context, context)) {
            return this;
        }
        return new Map2ListVariable<>(variable, name, id, context);
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.map2list, variable.toInfo());
    }

    /**
     * get the variable of the specified index in this list
     *
     * @param index index name
     * @return variable of the key
     */
    @Override
    protected Variable<Map.Entry<String, T>> variable(int index) {
        Variable<Map.Entry<String, T>> v = Lambda.getAt(variables(), index, null);
        if (v == null) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        return v;
    }

    /**
     * get all variables defined in this list
     *
     * @return the iterator
     */
    @Nonnull
    @Override
    protected Iterator<Variable<Map.Entry<String, T>>> variables() {
        return Lambda.map(variable.variables(), entry ->
            new TransformVariable<>(
                entry.getValue(),
                new ClosureWrapper<>(Closures.closure(null, (T value) ->
                    new AbstractMap.SimpleEntry<>(entry.getKey(), value)))
            )
        );
    }

    /**
     * if the list variable works in lazy mode
     *
     * @return return true if in lazy mode
     */
    @Override
    protected boolean lazyMode() {
        return variable.lazyMode();
    }

    /**
     * estimate the size of this list
     *
     * @return size
     */
    @Override
    protected int estimateSize() {
        return variable.estimateSize();
    }

    /**
     * Returns the number of elements in this list.  If this list contains
     * more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return variable.size();
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    @Override
    public boolean isEmpty() {
        return variable.isEmpty();
    }

    /**
     * Appends the specified element to the end of this list (optional
     * operation).
     *
     * <p>Lists that support this operation may place limitations on what
     * elements may be added to this list.  In particular, some
     * lists will refuse to add null elements, and others will impose
     * restrictions on the type of elements that may be added.  List
     * classes should clearly specify in their documentation any restrictions
     * on what elements may be added.
     *
     * @param o element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     * @throws UnsupportedOperationException if the <tt>add</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of the specified element
     *                                       prevents it from being added to this list
     * @throws NullPointerException          if the specified element is null and this
     *                                       list does not permit null elements
     * @throws IllegalArgumentException      if some property of this element
     *                                       prevents it from being added to this list
     */
    @Override
    public boolean add(Map.Entry<String, T> o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element (optional operation).
     *
     * @param index   index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws UnsupportedOperationException if the <tt>set</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of the specified element
     *                                       prevents it from being added to this list
     * @throws NullPointerException          if the specified element is null and
     *                                       this list does not permit null elements
     * @throws IllegalArgumentException      if some property of the specified
     *                                       element prevents it from being added to this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     *                                       (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    @Override
    public Map.Entry<String, T> set(int index, Map.Entry<String, T> element) {
        throw new UnsupportedOperationException();
    }
}
