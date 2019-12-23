package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.ConnectionChannel;
import com.yit.deploy.core.info.EnvironmentInfo;
import com.yit.deploy.core.info.HostGroupInfo;
import com.yit.deploy.core.info.HostInfo;

import java.util.List;
import java.util.Map;

public class HostContext extends BaseContext {
    private final HostInfo host;

    public HostContext(HostInfo host, EnvironmentInfo env, Map<String, EnvironmentInfo> envs) {
        this.host = host;
        resolveVars(env.getFullVars(envs));
    }

    public HostContext channel(ConnectionChannel value) {
        host.setChannel(value);
        return this;
    }

    public HostContext user(String value) {
        host.setUser(value);
        return this;
    }

    public HostContext retired() {
        host.setRetired(true);
        return this;
    }

    public HostContext port(int value) {
        host.setPort(value);
        return this;
    }

    public HostContext labels(Map<String, Object> labels) {
        host.getLabels().putAll(labels);
        return this;
    }

    public HostContext label(String name, Object value) {
        host.getLabels().put(name, value);
        return this;
    }

    public HostContext desc(String description) {
        host.setDescription(description);
        return this;
    }
}
