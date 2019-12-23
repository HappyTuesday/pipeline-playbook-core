package com.yit.deploy.core.compile;

import com.yit.deploy.core.function.ClosureWrapper;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;

public class DeployCompiler {

    private static final DeployCompiler INSTANCE = new DeployCompiler();

    public final DeployClassloader DEPLOY_CLASSLOADER = new DeployClassloader(DeployCompiler.class.getClassLoader());

    public static DeployCompiler getInstance() {
        return INSTANCE;
    }

    public final ClosureRecorder closureRecorder = new ClosureRecorder();

    private final GroovyShell GROOVY_SHELL = new GroovyShell(
        DEPLOY_CLASSLOADER,
        new CompilerConfiguration().addCompilationCustomizers(closureRecorder)
    );

    public GroovyShell getGroovyShell() {
        return GROOVY_SHELL;
    }

    public Script parseScript(String text) {
        return GROOVY_SHELL.parse(text);
    }

    public Script createScript(Class<Script> scriptClass) {
        return InvokerHelper.createScript(scriptClass, GROOVY_SHELL.getContext());
    }

    public <T> ClosureWrapper<T> parseClosure(String text) {
        if (text == null) {
            return null;
        }

        //noinspection unchecked
        Closure<T> closure = (Closure<T>) GROOVY_SHELL.evaluate("return " + text);
        return new ClosureWrapper<>(closure);
    }
}
