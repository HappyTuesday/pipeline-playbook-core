package com.yit.deploy.core.variables;

import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.variable.Variable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class SimpleVariables implements Variables {

    private final ComplexWrapper root;

    public SimpleVariables() {
        this.root = new ComplexWrapper();
    }

    public SimpleVariables(Iterable<VariableInfo> infos) {
        this();
        for (VariableInfo info : infos) {
            this.put(info.toVariable(Object.class));
        }
    }

    public SimpleVariables(SimpleVariables that) {
        this.root = new ComplexWrapper(that.root);
    }

    /**
     * check if this variable table is empty
     *
     * @return return true if we are empty
     */
    @Override
    public boolean isEmpty() {
        return root.isEmpty();
    }

    /**
     * check if the variable with given name is hidden by this variable table,
     * a variable is hidden only if there is a variable with the same name or its parent name
     *
     * @param name    variable name
     * @return true if the name is hidden
     */
    @Override
    public boolean hidden(@Nonnull VariableName name) {
        Wrapper w = root;
        for (String field : name.path) {
            w = ((ComplexWrapper) w).field(field);
            if (w == null) {
                return false;
            }
            if (!(w instanceof ComplexWrapper) || w.variable != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * get a variable by its name, can only get non-repeatable variables
     * including invisible variable
     *
     * @param name    variable name
     * @return retrieved variable
     */
    @Nullable
    @Override
    public <T> Variable<T> getWithInvisible(@Nonnull VariableName name) {
        Wrapper w = root;
        boolean invisible = false;
        for (String field : name.path) {
            if (w instanceof ComplexWrapper) {
                invisible |= w.variable != null;
                w = ((ComplexWrapper) w).field(field);
                if (w == null) {
                    //noinspection unchecked
                    return invisible ? INVISIBLE : null;
                }
            } else {
                //noinspection unchecked
                return INVISIBLE;
            }
        }
        //noinspection unchecked
        return invisible && w.variable == null ? (Variable<T>) INVISIBLE : (Variable<T>) w.variable;
    }

    /**
     * get all fields defined under the variable of the name,
     * in the order at which the variables are put
     *
     * @param name    variable name
     * @return an iterator iterating all fetched variables
     */
    @Nonnull
    @Override
    public <T> Iterator<Variable<T>> fields(@Nonnull VariableName name) {
        Wrapper w = navigate(name);
        if (w instanceof ComplexWrapper) {
            return ((ComplexWrapper) w).fields();
        } else {
            return Lambda.emptyIterator();
        }
    }

    /**
     * get all fields defined under the variable of the name,
     * in the reverse order at which the variables are put
     *
     * @param name    variable name
     * @return an iterator iterating all fetched variables
     */
    @Nonnull
    @Override
    public <T> Iterator<Variable<T>> reverseFields(@Nonnull VariableName name) {
        Wrapper w = navigate(name);
        if (w instanceof ComplexWrapper) {
            return ((ComplexWrapper) w).reverseFields();
        } else {
            return Lambda.emptyIterator();
        }
    }

    /**
     * iterate all variables, in the order at which the variables are put
     *
     * @return iterator
     */
    @Nonnull
    @Override
    public Iterator<Variable> variables() {
        return root.travel();
    }

    /**
     * iterate all variables (does not use hidden policy), in the order at which the variable are put
     *
     * @return iterator
     */
    @Override
    public Iterator<Variable> allVariables() {
        return root.travel();
    }

    /**
     * get the writable variable table which will be used to put variable
     *
     * @return writable variable table
     */
    @Override
    public Variables getWritable() {
        return this;
    }

    /**
     * put a variable
     *
     * @param variable the variable to put
     */
    @Override
    public <T> void put(@Nonnull Variable<T> variable) {
        createDir(variable.name()).put(variable);
    }

    /**
     * clear all variables in this table
     */
    @Override
    public void clear() {
        root.clear();
    }

    private ComplexWrapper createDir(@Nonnull VariableName name) {
        ComplexWrapper w = root;

        for (int i = 0; i < name.path.length - 1; i++) {
            String field = name.path[i];
            Wrapper c = w.field(field);

            if (c instanceof ComplexWrapper) {
                w = (ComplexWrapper) c;
            } else {
                w = w.dir(field, c == null ? null : c.variable);
            }
        }

        return w;
    }

    /**
     * navigate to the final node with the variable name
     * @param name variable name
     * @return final node or null if not find
     */
    private Wrapper navigate(VariableName name) {
        Wrapper w = root;
        for (String field : name.path) {
            if (w instanceof ComplexWrapper) {
                w = ((ComplexWrapper) w).field(field);
                if (w == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return w;
    }

    private static class Wrapper {
        public final Variable variable;

        Wrapper(Variable variable) {
            this.variable = variable;
        }
    }

    private static class ComplexWrapper extends Wrapper {
        private Node head;
        private Node tail;
        private final Map<String, Node> map = new LinkedHashMap<>();

        ComplexWrapper() {
            super(null);
        }

        ComplexWrapper(Variable variable) {
            super(variable);
        }

        ComplexWrapper(ComplexWrapper that) {
            super(that.variable);
            for (Node n = that.head; n != null; n = n.next) {
                Wrapper w;
                if (n.wrapper instanceof ComplexWrapper) {
                    w = new ComplexWrapper((ComplexWrapper) n.wrapper);
                } else {
                    w = n.wrapper;
                }

                Node m = new Node(w);
                if (this.head == null) {
                    this.head = this.tail = m;
                } else {
                    this.tail.next = m;
                    m.prev = this.tail;
                    this.tail = m;
                }
                VariableName name = m.wrapper.variable.name();
                if (!name.repeatable()) {
                    this.map.put(name.last(), m);
                }
            }
        }

        public ComplexWrapper dir(String name, Variable var) {
            removeNode(name);
            ComplexWrapper w = new ComplexWrapper(var);
            Node n = new Node(w);
            insertNode(n);
            map.put(name, n);
            return w;
        }

        public void put(Variable var) {
            if (var.name().repeatable()) {
                insertNode(new Node(new Wrapper(var)));
            } else {
                String name = var.name().last();
                removeNode(name);
                Node n = new Node(new Wrapper(var));
                insertNode(n);
                map.put(name, n);
            }
        }

        private void insertNode(Node n) {
            if (tail != null) {
                tail.next = n;
                n.prev = tail;
            }
            tail = n;
            if (head == null) {
                head = n;
            }
        }

        private void removeNode(String name) {
            Node n = map.get(name);
            if (n == null) {
                return;
            }

            if (n.next == null) {
                if (n != tail) {
                    throw new IllegalStateException();
                }
                tail = n.prev;
            } else {
                n.next.prev = n.prev;
            }

            if (n.prev == null) {
                if (n != head) {
                    throw new IllegalStateException();
                }
                head = n.next;
            } else {
                n.prev.next = n.next;
            }
        }

        public boolean isEmpty() {
            return head == null;
        }

        /**
         * iterate all variables
         *
         * @return variables
         */
        public Iterator<Variable> travel() {
            return Lambda.iterate(new Supplier<Variable>() {
                Iterator<Variable> iter;
                Node n = head;

                @Override
                public Variable get() {
                    if (iter == null) {
                        // ensure next loop will not come here
                        iter = Lambda.emptyIterator();
                        if (variable != null) {
                            return variable;
                        }
                    }

                    while (true) {
                        if (iter.hasNext()) {
                            return iter.next();
                        }
                        if (n == null) {
                            return null;
                        }
                        if (n.wrapper instanceof ComplexWrapper) {
                            iter = ((ComplexWrapper) n.wrapper).travel();
                            n = n.next;
                        } else {
                            Variable v = n.wrapper.variable;
                            n = n.next;
                            return v;
                        }
                    }
                }
            });
        }

        public Wrapper field(String name) {
            Node n = map.get(name);
            return n == null ? null : n.wrapper;
        }

        /**
         * iterate all fields
         *
         * @return field variables
         */
        public <T> Iterator<Variable<T>> fields() {
            return Lambda.iterate(new Supplier<Variable<T>>() {
                Node n = head;

                @Override
                public Variable<T> get() {
                    while (n != null) {
                        Variable var = n.wrapper.variable;
                        n = n.next;
                        if (var != null) {
                            //noinspection unchecked
                            return (Variable<T>) var;
                        }
                    }
                    return null;
                }
            });
        }

        /**
         * iterate all fields
         *
         * @return field variables
         */
        public <T> Iterator<Variable<T>> reverseFields() {
            return Lambda.iterate(new Supplier<Variable<T>>() {
                Node n = tail;

                @Override
                public Variable<T> get() {
                    while (n != null) {
                        Variable var = n.wrapper.variable;
                        n = n.prev;
                        if (var != null) {
                            //noinspection unchecked
                            return (Variable<T>) var;
                        }
                    }
                    return null;
                }
            });
        }

        public void clear() {
            map.clear();
            head = null;
            tail = null;
        }

        private static class Node {
            Node prev;
            Node next;
            final Wrapper wrapper;

            Node(Wrapper wrapper) {
                this.wrapper = wrapper;
            }
        }
    }
}
