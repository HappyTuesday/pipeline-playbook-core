package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.model.StatStruct;
import com.yit.deploy.core.function.Closures;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * Created by nick on 14/09/2017.
 */
public class StatStep extends AbstractFileStep {

    public StatStep(JobExecutionContext context) {
        super(context);
    }

    public StatStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected StatStruct executeOverride() {
        assert targetHost != null;
        assert path != null && !path.isEmpty() && !"/".equals(path);
        return statFile(path);
    }

    public static class DslContext {

        private StatStep step;

        public DslContext(StatStep step) {
            this.step = step;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext path(String value) {
            step.path = value;
            return this;
        }

        public DslContext target(String value) {
            return path(value);
        }
    }
}
