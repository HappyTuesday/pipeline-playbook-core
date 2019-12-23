package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class FilterListVariable<T> extends ListVariable<T> {

    private final ListVariable<T> variable;
    private final ClosureWrapper<Boolean> predicate;

    public FilterListVariable(ListVariable<T> variable, ClosureWrapper<Boolean> predicate) {
        this(variable, predicate, null, null);
    }

    public FilterListVariable(ListVariable<T> variable, ClosureWrapper<Boolean> predicate, VariableName name, String id) {
        this(variable, predicate, name, id, null);
    }

    private FilterListVariable(ListVariable<T> variable, ClosureWrapper<Boolean> predicate, VariableName name, String id, ResolveContext context) {
        super(name, id, context);
        this.variable = variable.context(context);
        this.predicate = predicate;
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
    public FilterListVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new FilterListVariable<>(variable, predicate, name, id, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public FilterListVariable<T> context(ResolveContext context) {
        if (Objects.equals(this.context, context)) {
            return this;
        }
        return new FilterListVariable<>(variable, predicate, name, id, context);
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.filterList, variable.toInfo(), predicate.toInfo());
    }

    private boolean test(Object value) {
        return predicate.with(context.target, value);
    }

    /**
     * get the variable of the specified index in this list
     *
     * @param index index name
     * @return variable of the key
     */
    @Override
    protected Variable<T> variable(int index) {
        Variable<T> v = Lambda.getAt(variables(), index, null);
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
    protected Iterator<Variable<T>> variables() {
        return Lambda.filter(variable.variables(), v -> test(v.resolve(context)));
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
        return Lambda.size(variables());
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
    public boolean add(T o) {
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
    public T set(int index, T element) {
        throw new UnsupportedOperationException();
    }
}
