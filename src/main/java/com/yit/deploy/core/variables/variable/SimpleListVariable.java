package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Represents a list of variables.
 */
public class SimpleListVariable<T> extends ListVariable<T> {

    private final List<Variable<T>> list;

    public SimpleListVariable(List<T> list) {
        super(null, null, null);
        this.list = new ArrayList<>(list.size());
        for (T o : list) {
            this.list.add(Variable.toVariable(o).context(null));
        }
    }

    public SimpleListVariable(ListVariable<T> lv, ResolveContext context) {
        super(null, null, context);
        this.list = new ArrayList<>(lv.estimateSize());
        for (Iterator<Variable<T>> iter = lv.variables(); iter.hasNext();) {
            this.list.add(iter.next().context(context));
        }
    }

    private SimpleListVariable(SimpleListVariable<T> that, ResolveContext context) {
        super(null, null, context);
        this.list = Lambda.map(that.list, x -> x.context(context));
    }

    /**
     * create a new variable instance with a different name
     *
     * if we have a name, we should change to a cascade list, this happens when saving a list to variable table
     * as a top level variable
     * <p>
     * NOTE: name field must be readonly, any change to the name field of a variable will create a new instance
     *
     * @param name the new variable name
     * @return new created variable
     */
    @Override
    public CascadeListVariable<T> name(@Nonnull VariableName name) {
        return new CascadeListVariable<>(name, id, list, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public SimpleListVariable<T> context(ResolveContext context) {
        if (Objects.equals(this.context, context)) {
            return this;
        }
        return new SimpleListVariable<>(this, context);
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.simpleList, Lambda.map(list, Variable::toInfo));
    }

    /**
     * get the variable of the specified index in this list
     *
     * @param index index name
     * @return variable of the key
     */
    @Override
    protected Variable<T> variable(int index) {
        return list.get(index);
    }

    /**
     * get all variables defined in this list
     *
     * @return the iterator
     */
    @Nonnull
    @Override
    protected Iterator<Variable<T>> variables() {
        return list.iterator();
    }

    /**
     * if the list variable works in lazy mode
     *
     * @return return true if in lazy mode
     */
    @Override
    protected boolean lazyMode() {
        return false;
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * estimate the size of this list
     *
     * @return size
     */
    @Override
    protected int estimateSize() {
        return list.size();
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
        return list.size();
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
     * @param t element to be appended to this list
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
    public boolean add(T t) {
        return list.add(Variable.toVariable(t).context(context));
    }

    /**
     * Inserts the specified element at the specified position in this list
     * (optional operation).  Shifts the element currently at that position
     * (if any) and any subsequent elements to the right (adds one to their
     * indices).
     *
     * @param index   index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws UnsupportedOperationException if the <tt>add</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of the specified element
     *                                       prevents it from being added to this list
     * @throws NullPointerException          if the specified element is null and
     *                                       this list does not permit null elements
     * @throws IllegalArgumentException      if some property of the specified
     *                                       element prevents it from being added to this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     *                                       (<tt>index &lt; 0 || index &gt; size()</tt>)
     */
    @Override
    public void add(int index, T element) {
        list.add(index, Variable.toVariable(element).context(context));
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element (optional operation).
     * If the passed index is bigger than the max index in this list,
     * it will cause the list to be extended.
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
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0</tt>)
     */
    @Override
    public T set(int index, T element) {
        list.set(index, Variable.toVariable(element).context(context));
        return null;
    }

    /**
     * Removes all of the elements from this list (optional operation).
     * The list will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *                                       is not supported by this list
     */
    @Override
    public void clear() {
        list.clear();
    }

    /**
     * Removes the element at the specified position in this list (optional
     * operation).  Shifts any subsequent elements to the left (subtracts one
     * from their indices).  Returns the element that was removed from the
     * list.
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *                                       is not supported by this list
     * @throws IndexOutOfBoundsException     if the index is out of range
     *                                       (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    @Override
    public T remove(int index) {
        list.remove(index);
        return null;
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * The specified index indicates the first element that would be
     * returned by an initial call to {@link ListIterator#next next}.
     * An initial call to {@link ListIterator#previous previous} would
     * return the element with the specified index minus one.
     *
     * @param index index of the first element to be returned from the
     *              list iterator (by a call to {@link ListIterator#next next})
     * @return a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index > size()})
     */
    @Nonnull
    @Override
    public ListIterator<T> listIterator(int index) {
        return Lambda.map(list.listIterator(index), v -> v.resolve(context));
    }
}
