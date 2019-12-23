package com.yit.deploy.core.function;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;
import groovy.transform.stc.ThirdParam;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class Closures {
    /**
     * Allows the closure to be called for the object reference self.
     * <p>
     * Any method invoked inside the closure will first be invoked on the
     * self reference. For instance, the following method calls to the append()
     * method are invoked on the StringBuilder instance:
     * <pre class="groovyTestCase">
     * def b = new StringBuilder().with {
     *   append('foo')
     *   append('bar')
     *   return it
     * }
     * assert b.toString() == 'foobar'
     * </pre>
     * This is commonly used to simplify object creation, such as this example:
     * <pre>
     * def p = new Person().with {
     *   firstName = 'John'
     *   lastName = 'Doe'
     *   return it
     * }
     * </pre>
     *
     * @param self    the object to have a closure act upon
     * @param closure the closure to call on the object
     * @return result of calling the closure
     * @since 1.5.0
     */
    public static <T,U> T with(
            @DelegatesTo.Target("self") U self,
            @DelegatesTo(target="self", strategy= Closure.DELEGATE_FIRST)
            @ClosureParams(FirstParam.class)
                    Closure<T> closure,
                    Object... args) {
        @SuppressWarnings("unchecked")
        final Closure<T> clonedClosure = (Closure<T>) closure.clone();
        clonedClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        clonedClosure.setDelegate(self);
        return clonedClosure.call(args);
    }

    public static <U> void delegateOnly(
        @DelegatesTo.Target("self") U self,
        @DelegatesTo(target="self", strategy=Closure.DELEGATE_ONLY)
            Closure closure) {
        withDelegateOnly(self, closure, null);
    }

    public static <T,U> T withDelegateOnly(
            @DelegatesTo.Target("self") U self,
            @DelegatesTo(target="self", strategy=Closure.DELEGATE_ONLY)
            @ClosureParams(ThirdParam.class)
                    Closure<T> closure) {
        return withDelegateOnly(self, closure, null);
    }

    public static <T,U,P> T withDelegateOnly(
            @DelegatesTo.Target("self") U self,
            @DelegatesTo(target="self", strategy=Closure.DELEGATE_ONLY)
            @ClosureParams(ThirdParam.class)
                    Closure<T> closure,
            P param) {
        @SuppressWarnings("unchecked")
        final Closure<T> clonedClosure = (Closure<T>) closure.clone();
        clonedClosure.setResolveStrategy(Closure.DELEGATE_ONLY);
        clonedClosure.setDelegate(self);
        return clonedClosure.call(param);
    }

    public static <V> Closure<V> closure(Object owner, Action f) {
        return closureVariant(owner, x -> {f.run(); return null;});
    }

    public static <V> Closure<V> closure(Object owner, Supplier<V> f) {
        return closureVariant(owner, x -> f.get());
    }

    @SuppressWarnings("unchecked")
    public static <P, V> Closure<V> closure(Object owner, Function<P, V> f) {
        return closureVariant(owner, (Object[] args) ->
                f.apply(args != null && args.length > 0 ? (P) args[0] : null));
    }

    public static <V> Closure<V> closureVariant(Object owner, Function<Object[], V> f) {
        return closure(owner, (o, d, args) -> f.apply(args));
    }

    public static <OWNER, DELEGATE, RESULT> Closure<RESULT> closure(Object owner, ClosureFunction<OWNER, DELEGATE, RESULT> f) {
        return new Closure<RESULT>(owner) {
            @SuppressWarnings("unchecked")
            public RESULT doCall(Object ... args) {
                return f.apply((OWNER) getOwner(), (DELEGATE) getDelegate(), args);
            }
        };
    }

    public static <DELEGATE, RESULT> Closure<RESULT> delegateClosure(Function<DELEGATE, RESULT> f) {
        return delegateClosure(f, f);
    }

    public static <DELEGATE, RESULT> Closure<RESULT> delegateClosure(Object owner, Function<DELEGATE, RESULT> f) {
        return Closures.closure(owner, (Object o, DELEGATE d, Object ... args) -> f.apply(d));
    }

    public static RuntimeException wrapException(Throwable t) {
        return t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
    }
}
