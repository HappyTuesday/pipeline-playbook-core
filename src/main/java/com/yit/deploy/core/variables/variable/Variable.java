package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.dsl.parse.VariableContext;
import com.yit.deploy.core.exceptions.AccessDeniedVariableException;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import groovy.lang.Closure;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * represent a variable
 */
public interface Variable<T> {

    /**
     * the name of the variable
     * @return variable name
     */
    VariableName name();

    /**
     * create a new variable instance with a different name
     *
     * NOTE: name field must be readonly, any change to the name field of a variable will create a new instance
     *
     * @param name the new variable name
     * @return new created variable
     */
    Variable<T> name(@Nonnull VariableName name);

    /**
     * get the id of the variable
     * @return id
     */
    String id();

    /**
     * with the specified context
     *
     * @param context resolve context
     * @return new created context
     */
    default Variable<T> context(ResolveContext context) {
        return this;
    }

    /**
     * resolve the value of this variable
     *
     * @param context context used when resolving
     * @return the retrieved value
     */
    default T resolve(ResolveContext context) {
        if (context != null && context.authorizer != null && !context.authorizer.authorize(this)) {
            throw new AccessDeniedVariableException(this);
        }
        return get(context);
    }

    /**
     * resolve and return a variable-free object
     *
     * @param context context used when resolving
     * @return value
     */
    default Object concrete(ResolveContext context) {
        return concreteObject(resolve(context));
    }

    static Object concreteObject(Object o) {
        if (o instanceof List) {
            return concreteList((List<?>) o);
        }
        if (o instanceof Map) {
            return concreteMap((Map<?, ?>) o);
        }
        return o;
    }

    static List<Object> concreteList(List<?> list) {
        List<Object> result = new ArrayList<>(list instanceof ListVariable ?
            ((ListVariable<?>) list).estimateSize() : list.size());

        for (Object o : list) {
            result.add(concreteObject(o));
        }
        return result;
    }

    static Map<?, Object> concreteMap(Map<?, ?> map) {
        Map<Object, Object> result = new LinkedHashMap<>(map instanceof MapVariable ?
            ((MapVariable<?>) map).estimateSize() : map.size());

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(entry.getKey(), concreteObject(entry.getValue()));
        }
        return result;
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     * @param context resolving context
     * @return the value of the variable
     */
    T get(ResolveContext context);

    /**
     * convert variable to variable info
     * @return variable info
     */
    VariableInfo toInfo();

    /**
     * prepare for save
     * @param name the name of the variable to save
     * @return the version of this variable to save
     */
    default Variable<T> saving(VariableName name) {
        return name(name);
    }

    /**
     * get the last field of the name
     * @return field name
     */
    default String field() {
        return name().last();
    }

    /**
     * wrap a value to variable if it is not an variable or variable context
     * @param closure variable value
     * @return variable
     */
    static <T> LazyVariable<T> lazyVariable(ClosureWrapper<T> closure) {
        return (LazyVariable<T>) toVariable(closure);
    }

    /**
     * wrap a value to variable if it is not an variable or variable context
     * @param list variable value
     * @return variable
     */
    @SuppressWarnings("unchecked")
    static <T> ListVariable<T> listVariable(List<T> list) {
        return (ListVariable<T>) (Variable) toVariable(list);
    }

    /**
     * wrap a value to variable if it is not an variable or variable context
     * @param list variable value
     * @return variable
     */
    @SuppressWarnings("unchecked")
    static <T> MapVariable<T> mapVariable(Map<String, T> list) {
        return (MapVariable<T>) (Variable) toVariable(list);
    }

    /**
     * wrap a value to variable if it is not an variable or variable context
     * @param value variable value
     * @return variable
     */
    @SuppressWarnings("unchecked")
    static <T> Variable<T> toVariableCast(Object value) {
        return (Variable<T>) toVariable(value);
    }

    static Object unwrapContext(Object value) {
        if (value instanceof VariableContext) {
            return ((VariableContext) value).variable;
        }
        return value;
    }

    /**
     * wrap a value to variable if it is not an variable or variable context
     * @param value variable value
     * @return variable
     */
    @SuppressWarnings("unchecked")
    static <T> Variable<T> toVariable(T value) {
        if (value instanceof Variable) {
            return (Variable<T>) value;
        }
        if (value instanceof VariableContext<?>) {
            return ((VariableContext) value).variable;
        }
        if (value instanceof List) {
            return new SimpleListVariable<>((List) value);
        }
        if (value instanceof Map) {
            return new SimpleMapVariable<>((Map) value);
        }
        if (value instanceof ClosureWrapper) {
            return new LazyVariable((ClosureWrapper) value);
        }
        if (value instanceof Closure) {
            return new LazyVariable<>(new ClosureWrapper<>((Closure) value));
        }
        return new SimpleVariable(value);
    }

    /**
     * parse a variable from text, first the text will be deserialized to variable info, then converted to variable
     * @param text text
     * @param <T> variable element type
     * @return variable
     */
    @SuppressWarnings("unchecked")
    static <T> Variable<T> parse(@Nullable String text) {
        if (text == null) {
            return null;
        }
        return (Variable<T>) VariableInfo.parse(text).toVariable();
    }
}
