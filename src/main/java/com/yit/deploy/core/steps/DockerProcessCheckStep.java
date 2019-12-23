package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.model.RemoteProcessLauncher;
import com.yit.deploy.core.function.Closures;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by nick on 14/09/2017.
 */
public class DockerProcessCheckStep extends AbstractStep {
    private Host targetHost;
    private String projectName;
    private String status;
    private List<Integer> pids;

    private static final String RUNNING = "running";
    private static final String STOPPED = "stopped";
    private static final String SAVEPID = "save-pid";

    public DockerProcessCheckStep(JobExecutionContext context) {
        super(context);
    }

    public DockerProcessCheckStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Void executeOverride() {
        assert targetHost != null;
        assert Arrays.asList(SAVEPID, RUNNING, STOPPED).contains(status);
        assert projectName != null;

        if (getEnv().isLocalEnv()) return null;

        if (SAVEPID.equals(status) || pids == null) {
            String composeProjectName = getDockerComposeProjectName(projectName);
            List<String> cids = Lambda.tokenize(ssh("docker ps -aqf label=com.docker.compose.project=" + composeProjectName), "\n");
            pids = Collections.emptyList();
            if (!cids.isEmpty()) {
                pids = Lambda.map(Lambda.tokenize(ssh("docker inspect -f '{{.State.Pid}}' " + String.join(" ", cids)), "\n"), Integer::parseInt);
            }
            if (pids.size() != cids.size()) {
                throw new RuntimeException("could not inspect the pid for all containers " + cids + ", in fact, got " + pids);
            }
            pids = Lambda.findAll(pids, pid -> pid > 0);
        }

        if (SAVEPID.equals(status)) return null;

        List<Integer> alivePids = Collections.emptyList();
        if (!pids.isEmpty()) {
            alivePids = Lambda.map(Lambda.tokenize(ssh("ps -ho pid -p 1," + Lambda.join(",", pids)), "\n"), s -> Integer.parseInt(s.trim()));
            alivePids = Lambda.findAll(alivePids, pid -> pid != 1);
        }
        if (RUNNING.equals(status)) {
            if (pids.isEmpty()) {
                throw new RuntimeException("could not find docker container with docker-compose project " + projectName);
            }
            if (alivePids.size() != pids.size()) {
                throw new RuntimeException("process(es) " + Lambda.toString(Lambda.except(pids, alivePids)) + " are/is not running");
            }
        } else if (STOPPED.equals(status)) {
            if (!alivePids.isEmpty()) {
                throw new RuntimeException("process(es) " + alivePids + " are/is still running");
            }
        }
        return null;
    }

    private String ssh(String shell) {
        return new RemoteProcessLauncher(targetHost, shell).launcher.script(getScript()).executeReturnText();
    }

    private static String getDockerComposeProjectName(String projectName) {
        return projectName.replaceAll("[^a-zA-Z]+", "");
    }

    public static class DslContext {

        private DockerProcessCheckStep step;

        public DslContext(DockerProcessCheckStep step) {
            this.step = step;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext projectName(String value) {
            step.projectName = value;
            return this;
        }

        public DslContext running() {
            step.status = RUNNING;
            return this;
        }

        public DslContext stopped() {
            step.status = STOPPED;
            return this;
        }

        public DslContext savePID() {
            step.status = SAVEPID;
            return this;
        }
    }
}
