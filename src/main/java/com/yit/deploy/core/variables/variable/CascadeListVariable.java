package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Supplier;

/**
 * Represents a list of variables.
 *
 */
public class CascadeListVariable<T> extends ListVariable<T> {

    private final List<Variable<T>> list;

    public CascadeListVariable(@Nonnull VariableName name, String id, Iterable<Variable<T>> inside, ResolveContext context) {
        super(name, id, context);
        this.list = new ArrayList<>();
        int i = 0;
        for (Variable<T> v : inside) {
            this.list.add(v.context(context).name(name.field(String.valueOf(i++))));
        }
    }

    private CascadeListVariable(CascadeListVariable<T> that, ResolveContext context) {
        super(that.name, that.id, context);
        this.list = Lambda.map(that.list, x -> x.context(context));
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
    public CascadeListVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new CascadeListVariable<>(name, id, this::variables, context);
    }

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    @Override
    public CascadeListVariable<T> context(ResolveContext context) {
        if (Objects.equals(this.context, context)) {
            return this;
        }
        return new CascadeListVariable<>(this, context);
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.cascadeList, Lambda.map(list, Variable::toInfo));
    }

    /**
     * if the list variable works in lazy mode
     *
     * @return return true if in lazy mode
     */
    @Override
    protected boolean lazyMode() {
        return true;
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

    private Variable<T> cascade(Variable<T> v) {
        Variable<T> vv = context.where.get(v.name());
        if (vv != null) {
            return vv.context(context);
        }
        return v;
    }

    /**
     * get the variable of the specified index in this list
     *
     * @param index index name
     * @return variable of the key
     */
    @Override
    protected Variable<T> variable(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        if (index < list.size()) {
            return cascade(list.get(index));
        }
        int i = index - list.size();
        VariableName iv = name.field(String.valueOf(index));
        for (Iterator<Variable<T>> iter = context.where.fields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof AppendedListVariable) {
                if (i == 0) {
                    return cascade(((AppendedListVariable<T>) v).variable.context(context).name(iv));
                } else {
                    i--;
                }
            } else if (v instanceof ExpandableListVariable) {
                ListVariable<T> lv = ((ExpandableListVariable<T>) v).variable.context(context);
                int s = lv.size();
                if (i < s) {
                    return cascade(lv.variable(i).name(iv));
                } else {
                    i -= s;
                }
            }
        }
        throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    /**
     * get all variables defined in this list
     *
     * @return the iterator
     */
    @Nonnull
    @Override
    protected Iterator<Variable<T>> variables() {
        return Lambda.iterate(new Supplier<Variable<T>>() {
            Iterator<Variable<T>> iter;
            Iterator<Variable<T>> superIter;
            int index = 0;

            @Override
            public Variable<T> get() {
                while (true) {
                    if (iter != null && iter.hasNext()) {
                        Variable<T> v = iter.next();
                        return cascade(v.context(context).name(name.field(String.valueOf(index++))));
                    } else if (superIter == null) {
                        iter = list.iterator();
                        superIter = context.where.fields(name);
                    } else if (superIter.hasNext()) {
                        Variable<T> v = superIter.next();

                        if (v instanceof AppendedListVariable) {

                            return cascade(((AppendedListVariable<T>) v).variable
                                .context(context)
                                .name(name.field(String.valueOf(index++))));

                        } else if (v instanceof ExpandableListVariable) {
                            iter = ((ExpandableListVariable<T>) v).variable.context(context).variables();
                        }
                    } else {
                        return null;
                    }
                }
            }
        });
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
        int s = list.size();
        for (Iterator<Variable<T>> iter = context.where.fields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof AppendedListVariable) {
                s++;
            } else if (v instanceof ExpandableListVariable) {
                s += ((ExpandableListVariable<T>) v).variable.context(context).size();
            }
        }
        return s;
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    @Override
    public boolean isEmpty() {
        if (!list.isEmpty()) {
            return false;
        }
        for (Iterator<Variable<T>> iter = context.where.fields(name); iter.hasNext();) {
            Variable<T> v = iter.next();
            if (v instanceof AppendedListVariable) {
                return false;
            } else if (v instanceof ExpandableListVariable) {
                if (!((ExpandableListVariable<T>) v).variable.context(context).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
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
        Variable<T> v = Variable.toVariable(t);
        if (v instanceof AppendedListVariable || v instanceof ExpandableListVariable) {
            // as is
        } else {
            v = new AppendedListVariable<>(v.context(null));
        }
        context.where.put(v.context(context).name(name.toRepeatable()));
        return true;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator (optional operation).  The behavior of this
     * operation is undefined if the specified collection is modified while
     * the operation is in progress.  (Note that this will occur if the
     * specified collection is this list, and it's nonempty.)
     *
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this list
     * @throws NullPointerException          if the specified collection contains one
     *                                       or more null elements and this list does not permit null
     *                                       elements, or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this list
     * @see #add(Object)
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        Object value = Variable.unwrapContext(c);
        if (value instanceof ListVariable) {
            ListVariable<T> lv = (ListVariable<T>) value;

            if (lv.lazyMode()) {
                context.where.put(new ExpandableListVariable<>(lv, name.toRepeatable(), lv.id));
            } else {
                for (Iterator<Variable<T>> iter = lv.context(context).variables(); iter.hasNext(); ) {
                    add((T) iter.next());
                }
            }

            return true;
        }

        for (T o : c) {
            add(o);
        }

        return true;
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
        context.where.put(Variable.toVariable(element).context(context).name(name.field(String.valueOf(index))));
        return null;
    }
}
