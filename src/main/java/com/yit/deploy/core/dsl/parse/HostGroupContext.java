package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.info.EnvironmentInfo;
import com.yit.deploy.core.info.HostGroupInfo;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.HostInfo;
import com.yit.deploy.core.model.Host;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.Map;

public class HostGroupContext extends BaseContext {
    private final EnvironmentInfo env;
    private final Map<String, EnvironmentInfo> envs;
    private final HostGroupInfo hostGroup;

    public HostGroupContext(EnvironmentInfo env, Map<String, EnvironmentInfo> envs, String name) {
        this.env = env;
        this.envs = envs;
        HostGroupInfo hostGroup = env.getHostGroups().get(name);
        if (hostGroup == null) {
            hostGroup = new HostGroupInfo();
            hostGroup.setName(name);
            env.getHostGroups().put(name, hostGroup);
        }
        this.hostGroup = hostGroup;

        resolveVars(env.getFullVars(envs));
    }

    public void desc(String description) {
        hostGroup.setDescription(description);
    }

    public void host(String hostname) {
        createHostIfNotExists(hostname);
        Lambda.uniqueAdd(hostGroup.getHosts(), hostname);
    }

    public HostGroupContext retired(String hostname) {
        createHostIfNotExists(hostname);
        Lambda.uniqueAdd(hostGroup.getHostsRetired(), hostname);
        return this;
    }

    public HostGroupContext inherits(String ... groups) {
        for (String group : groups) {
            validateGroupName(group);
            Lambda.uniqueAdd(hostGroup.getInherits(), group);
        }
        return this;
    }

    public HostGroupContext retiredInherits(String ... groups) {
        for (String group : groups) {
            validateGroupName(group);
            Lambda.uniqueAdd(hostGroup.getInheritsRetired(), group);
        }
        return this;
    }

    private void createHostIfNotExists(String hostname) {
        env.getHosts().computeIfAbsent(hostname, HostInfo::new);
    }

    private void validateGroupName(String group) {
        if (Lambda.all(env.descending(envs), e -> !envs.get(e).getHostGroups().containsKey(group))) {
            throw new IllegalConfigException("invalid host group name " + group);
        }
    }

    public HostGroupContext overrideHosts(boolean override) {
        hostGroup.setOverrideHosts(override);
        return this;
    }

    public HostGroupContext overrideHosts() {
        return overrideHosts(true);
    }
}
