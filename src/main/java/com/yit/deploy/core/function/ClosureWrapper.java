package com.yit.deploy.core.function;

import com.yit.deploy.core.compile.DeployCompiler;
import com.yit.deploy.core.info.ClosureInfo;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.Objects;

public class ClosureWrapper<T> {
    private final Closure<T> closure;
    private final String groovy;

    public ClosureWrapper(Closure<T> closure, String groovy) {
        this.closure = closure;
        this.groovy = groovy;
    }

    public ClosureWrapper(Closure<T> closure) {
        this.closure = closure;
        this.groovy = DeployCompiler.getInstance().closureRecorder.getClosureText(closure.getClass());
    }

    public T call() {
        return closure.call();
    }

    public T call(Object arguments) {
        return closure.call(arguments);
    }

    public T call(Object... args) {
        return closure.call(args);
    }

    public Closure<T> getClosure() {
        return closure;
    }

    public String getGroovy() {
        return groovy;
    }

    public ClosureInfo toInfo() {
        return new ClosureInfo(groovy);
    }

    public <U> T with(@DelegatesTo.Target("self") U self, Object... args) {
        return Closures.with(self, closure, args);
    }

    public <U> void delegateOnly(@DelegatesTo.Target("self") U self) {
        withDelegateOnly(self, null);
    }

    public <U> T withDelegateOnly(@DelegatesTo.Target("self") U self) {
        return withDelegateOnly(self, null);
    }

    public <U,P> T withDelegateOnly(@DelegatesTo.Target("self") U self, P param) {
        return Closures.withDelegateOnly(self, closure, param);
    }

    public static ClosureWrapper<Boolean> TO_BOOLEAN = new ClosureWrapper<>(
        Closures.closure(null, Objects::nonNull), "{it as boolean}"
    );
}
