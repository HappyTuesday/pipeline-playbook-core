package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.exceptions.ContinueWaitException;
import com.yit.deploy.core.exceptions.ProcessExecutionException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.model.RemoteProcessLauncher;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * We use registrator container to register all opened ports of a docker container started on the same host to ETCD service.
 * This step is designed to wrap all required logic to manage registrator and its registrated items in ETCD.
 * The target of this step is a container.
 *
 * Created by nick on 14/09/2017.
 */
public class RegisterEtcdStep extends AbstractStep {
    private List<String> serviceNames;
    private Host targetHost;

    private String keysAPI;

    private boolean doNotStopRegistrator;

    /**
     * The status of the ETCD item for the container. true means to create or keep. false means to remove
     */
    private boolean status = true;

    public RegisterEtcdStep(JobExecutionContext context) {
        super(context);
    }

    public RegisterEtcdStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     */
    @Override
    protected Object executeOverride() {
        keysAPI = getVariable("ETCD_API_URL") + "/keys";

        if (status) {
            startRegistrator();
            waitForKeysShownInEtcd();
        } else {
            if (doNotStopRegistrator) {
                getScript().debug("stopping registrator is skipped");
            } else {
                stopRegistrator();
            }

            deleteKeysFromEtcd();
//            getScript().sleep(10);
        }
        return null;
    }

    private void stopRegistrator() {
        ssh("docker ps -qf label=com.docker.compose.service=registrator | xargs -I= docker stop =");
        getScript().info("registrator is stopped");
    }

    private void startRegistrator() {
        try {
            ssh("docker start registrator");
            getScript().info("registrator is started");
        } catch (ProcessExecutionException e) {
            throw new RuntimeException("could not start docker container named registrator. please re-deploy the registrator project and then retry.");
        }
    }

    private void waitForKeysShownInEtcd() {
        new WaitForStep(context, () -> {
            List<String> keys = listEtcdRelatedKeys();
            if (keys.isEmpty()) {
                throw new ContinueWaitException("waiting for a key which will be registered in ETCD under any of the services " + serviceNames + "for host " + targetHost.getHostname());
            } else {
                getScript().info("ETCD key %s is registered for host %s", Lambda.toString(keys), targetHost.getHostname());
            }
            return null;
        }).execute();
    }

    private void deleteKeysFromEtcd() {
        for (String key : listEtcdRelatedKeys()) {
            String keyUrl = keysAPI + key;
            getScript().info("DELETE KEY FROM ETCD: " + keyUrl);
            UriStep uri = new UriStep(context);
            new UriStep.DslContext(uri).url(keysAPI + key).followRedirects().DELETE();
            uri.execute();
        }
    }

    private List<String> listEtcdRelatedKeys() {
        String hostname = targetHost.getHostname();
        boolean darwin = targetHost.isDarwin();

        List<String> result = new ArrayList<>();
        for (String serviceName : serviceNames) {
            UriStep uri = new UriStep(context);
            new UriStep.DslContext(uri).url(keysAPI + "/services/" + serviceName).followRedirects().statusCode(200, 404).returnContent();
            Map m = uri.execute(Map.class);
            Map n = (Map) m.get("node");
            if (n != null && n.containsKey("nodes")) {
                for (Object item : (List) n.get("nodes")) {
                    String key = (String) ((Map)item).get("key");
                    if (darwin || Objects.equals(Lambda.last(key.split("/")).split(":")[0], hostname)) {
                        result.add(key);
                    }
                }
            }
        }

        return result;
    }

    private void ssh(String shell) {
        new RemoteProcessLauncher(targetHost, shell).launcher.script(getScript()).executeIgnoreOutput();
    }

    public static class DslContext {

        private RegisterEtcdStep step;

        public DslContext(RegisterEtcdStep step) {
            this.step = step;
        }

        public DslContext targetHost(Host value) {
            step.targetHost = value;
            return this;
        }

        public DslContext serviceNames(List<String> value) {
            step.serviceNames = value;
            return this;
        }

        public DslContext register() {
            step.status = true;
            return this;
        }

        public DslContext deregister() {
            step.status = false;
            return this;
        }

        public DslContext doNotStopRegistrator() {
            step.doNotStopRegistrator = true;
            return this;
        }
    }
}
