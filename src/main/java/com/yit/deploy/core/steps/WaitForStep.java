package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.exceptions.ContinueWaitException;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.Host;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by nick on 14/09/2017.
 */
public class WaitForStep extends AbstractStep {
    private List<Supplier<?>> waits = new ArrayList<>();
    private int interval = 5;
    private int retries = 20;
    private boolean quiet;
    private Supplier<?> onExceed;

    public WaitForStep(JobExecutionContext context) {
        super(context);
    }

    public WaitForStep(JobExecutionContext context, Supplier<?> wait) {
        super(context);
        waits.add(wait);
    }

    public WaitForStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() {
        List<Supplier<?>> waits = new ArrayList<>(this.waits);
        Object result = null;
        for (int i = retries; true;) {
            if (waits.isEmpty()) break;

            Supplier<?> wait = waits.remove(waits.size() - 1);
            try {
                result = wait.get();
            } catch (ContinueWaitException e) {
                if (--i < 0) {
                    try {
                        if (onExceed!= null) {
                            onExceed.get();
                        }
                    } finally {
                        throw new RuntimeException("give up waiting after " + retries + " times retrying\n" + e.getMessage(), e.getCause());
                    }
                }
                if (!quiet) getScript().warn(e.getMessage());
                getScript().sleep(interval);
                waits.add(wait);
            }
        }
        return result;
    }

    public static class DslContext {

        private WaitForStep step;

        public DslContext(WaitForStep step) {
            this.step = step;
        }

        public DslContext closure(Closure<?> value) {
            step.waits.add(value::call);
            return this;
        }

        public DslContext interval(int seconds) {
            step.interval = seconds;
            return this;
        }

        public DslContext retries(int value) {
            step.retries = value;
            return this;
        }

        public DslContext quiet() {
            step.quiet = true;
            return this;
        }

        public  DslContext onExceed(Closure<?> value) {
            step.onExceed = (value::call);
            return this;
        }

        public DslContext waitForPort(Host host, int port) {
            return waitForPort(host, port, false);
        }

        public DslContext waitForPort(Host host, int port, boolean closed) {
            Supplier<?> wait = () -> {
                List<String> lines = Lambda.tokenize(host.ssh("sudo lsof -a -sTCP:LISTEN -iTCP:" + port + " -Fp", Arrays.asList(0, 1)), "\n");
                if (closed) {
                    if (!lines.isEmpty()) {
                        throw new ContinueWaitException("waiting for port " + port + " to close");
                    }
                } else {
                    if (lines.isEmpty()) {
                        throw new ContinueWaitException("waiting for port " + port + " to open");
                    }
                }
                return null;
            };

            step.waits.add(wait);
            return this;
        }
    }
}