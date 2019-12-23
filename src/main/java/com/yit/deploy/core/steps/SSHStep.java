package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.model.RemoteProcessLauncher;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.function.Closures;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * Created by nick on 14/09/2017.
 */
public class SSHStep extends AbstractStep {
    private Host targetHost;
    private String pwd;
    private String shell;
    private int timeout = -1;

    public SSHStep(JobExecutionContext context) {
        super(context);
    }

    public SSHStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() {
        assert targetHost != null && shell != null && !shell.isEmpty();
        new RemoteProcessLauncher(targetHost, shell, pwd).launcher.timeout(timeout).script(context.getScript()).executePrintOutput();
        return null;
    }

    public static class DslContext {

        private SSHStep step;

        public DslContext(SSHStep step) {
            this.step = step;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext pwd(String value) {
            step.pwd = value;
            return this;
        }

        public DslContext shell(String value) {
            step.shell = value;
            return this;
        }

        public DslContext timeout(int timeout) {
            step.timeout = timeout;
            return this;
        }
    }
}
