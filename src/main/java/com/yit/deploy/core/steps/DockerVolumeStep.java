package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.function.Closures;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * Created by nick on 14/09/2017.
 */
public class DockerVolumeStep extends AbstractStep {
    private Host targetHost;
    private String volumeName;
    private boolean present = true;

    public DockerVolumeStep(JobExecutionContext context) {
        super(context);
    }

    public DockerVolumeStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() {
        assert targetHost != null;
        assert volumeName != null && !volumeName.isEmpty();

        if (Lambda.tokenize(ssh("docker volume ls -q"), "\n").contains(volumeName)) {
            if (present) {
                // nothing to do
            } else {
                getScript().info("remove docker volume " + volumeName);
                ssh("docker volume rm '" + volumeName + "'");
            }
        } else {
            if (present) {
                getScript().info("create docker volume " + volumeName);
                ssh("docker volume create --name '" + volumeName + "'");
            } else {
                // nothing to do
            }
        }
        return null;
    }

    private String ssh(String shell) {
        return targetHost.ssh(shell);
    }

    public static class DslContext {

        private DockerVolumeStep step;

        public DslContext(DockerVolumeStep step) {
            this.step = step;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext name(String value) {
            step.volumeName = value;
            return this;
        }

        public DslContext delete() {
            step.present = false;
            return this;
        }
    }
}
