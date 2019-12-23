package com.yit.deploy.core.support;

import com.yit.deploy.core.dsl.parse.ListVariableContext;
import com.yit.deploy.core.dsl.parse.MapVariableContext;
import com.yit.deploy.core.dsl.parse.VariableContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.variables.variable.*;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;

import java.util.*;

/**
 * Created by nick on 01/09/2017.
 */
public interface VariableValueTypesSupport {

    default <T> VariableContext<T> variable(T value) {
        return new VariableContext<>(new SimpleVariable<>(value));
    }

    default <T> VariableContext<T> variable(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<T> closure) {
        return new VariableContext<>(new LazyVariable<>(new ClosureWrapper<>(closure)));
    }

    default <T> VariableContext<T> lazy(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<T> closure) {
        return new VariableContext<>(new LazyVariable<>(new ClosureWrapper<>(closure)));
    }

    default <T> VariableContext<T> abstractedVariable(Class<T> clazz) {
        return new VariableContext<>(new AbstractVariable<>());
    }

    default <T> VariableContext<Closure<T>> closureVariable(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<T> closure) {
        return new VariableContext<>(new ClosureVariable<>(new ClosureWrapper<>(closure)));
    }

    default <P1, T> VariableContext<Closure<T>> closureVariable(
            Class<P1> parameterType1,
            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class)
            @ClosureParams(FirstParam.FirstGenericType.class)
                    Closure<T> closure) {

        return new VariableContext<>(new ClosureVariable<>(new ClosureWrapper<>(closure)));
    }

    default <T> VariableContext<Closure<T>> closureVariable() {
        return new VariableContext<>(new ClosureVariable<>(null));
    }

    default <T> VariableContext<T> cachedVariable(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<T> closure) {
        return new VariableContext<>(new CachedVariable<>(new LazyVariable<>(new ClosureWrapper<>(closure))));
    }

    default <T> ListVariableContext<T> expandList(List<T> list) {
        return new ListVariableContext<>(new ExpandableListVariable<>(Variable.listVariable(list)));
    }

    default <T> ListVariableContext<T> expandList(T ... list) {
        return expandList(Arrays.asList(list));
    }

    default <T> ListVariableContext<T> expandList(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<List<T>> closure) {
        return new ListVariableContext<>(
            new ExpandableListVariable<>(new LazyListVariable<>(Variable.lazyVariable(new ClosureWrapper<>(closure))))
        );
    }

    default <T> ListVariableContext<T> listVariable() {
        return new ListVariableContext<>(new SimpleListVariable<>(new ArrayList<>()));
    }

    default <T> ListVariableContext<T> listVariable(List<T> list) {
        return new ListVariableContext<>(new SimpleListVariable<>(list));
    }

    default <T> ListVariableContext<T> listvar(
            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<List<T>> closure) {
        return new ListVariableContext<>(new LazyListVariable<>(Variable.lazyVariable(new ClosureWrapper<>(closure))));
    }

    default <K, V> MapVariableContext<K, V> mapVariable() {
        return new MapVariableContext<>(new SimpleMapVariable<>(new HashMap<>()));
    }

    default <K, V> MapVariableContext<K, V> mapVariable(Map<K, V> list) {
        return new MapVariableContext<>(Variable.toVariable(list));
    }

    default <K, V> MapVariableContext<K, V> mapvar(
            @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<Map<String, V>> closure) {
        return new MapVariableContext<>(new LazyMapVariable<>(Variable.lazyVariable(new ClosureWrapper<>(closure))));
    }

    default <T> ListVariableContext<T> listOptional(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<T> closure) {
        ListVariableContext<T> context = new ListVariableContext<>(
            new SimpleListVariable<>(Collections.singletonList(Variable.lazyVariable(new ClosureWrapper<>(closure))))
        );
        return context.grep().expand();
    }
}
