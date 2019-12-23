package com.yit.deploy.core.model;

import com.yit.deploy.core.algorithm.Merger;
import com.yit.deploy.core.algorithm.QueryExpression;
import com.yit.deploy.core.dsl.evaluate.EnvironmentEvaluationContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.EnvironmentInfo;
import com.yit.deploy.core.info.HostGroupInfo;
import com.yit.deploy.core.variables.*;
import com.yit.deploy.core.variables.resolvers.SimpleVariableResolver;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;

public class Environment {

    public static final String DEFAULT_DEPLOY_USER_VARIABLE = "$DEFAULT_DEPLOY_USER";

    public static final String LOCAL_ENV_LABEL = "local";
    public static final String TEST_ENV_LABEL = "testenv";
    public static final String PROD_ENV_LABEL = "prod";

    /**
     * the environment name
     */
    private final String name;

    /**
     * if this environment abstracted
     */
    private final boolean abstracted;

    /**
     * the description of the env
     */
    private final String description;

    /**
     * the path from the most top parent to this env
     */
    private final List<Environment> descending;
    /**
     * all variables defined in this environment and its parents
     */
    private final LayeredVariables vars;
    /**
     * labels included in this environment and all its parents
     * used for environment query and filter
     */
    private final List<String> labels;
    /**
     * uesrname used as the default name to ssh login to target server
     */
    private final String defaultDeployUser;
    /**
     * all hosts defined in this environment and all its parents
     */
    private final Hosts hosts;
    /**
     * all host groups defined in this environment and its parents
     * the hosts contained in this hostGroups is equal to the hosts field
     */
    private final HostGroups hostGroups;

    public Environment(String name, Map<String, EnvironmentInfo> infoMap, Environments envs) {
        if (name == null) {
            throw new IllegalConfigException("environment name is not set");
        }
        EnvironmentInfo info = infoMap.get(name);
        if (info == null) {
            throw new IllegalConfigException("could not find environment info " + name);
        }

        this.name = info.getName();
        this.abstracted = info.isAbstracted() != null && info.isAbstracted();
        this.description = info.getDescription();

        List<EnvironmentInfo> infoDescending = Lambda.map(info.descending(infoMap), infoMap::get);
        this.descending = Lambda.map(infoDescending, x -> x == info ? this : envs.get(x.getName()));

        this.vars = new LayeredVariables();
        for (EnvironmentInfo e : infoDescending) {
            if (!e.getVars().isEmpty()) {
                this.vars.layer(e.getVars());
            }
        }

        this.labels = new ArrayList<>(Lambda.collectSet(infoDescending, EnvironmentInfo::getLabels));

        this.defaultDeployUser = new SimpleVariableResolver(this.vars).getVariableOrDefault(DEFAULT_DEPLOY_USER_VARIABLE, null);

        this.hosts = new Hosts(Lambda.collectMap(infoDescending, EnvironmentInfo::getHosts), this);

        Map<String, HostGroupInfo> hostGroupInfos = new HashMap<>();
        for (EnvironmentInfo e : infoDescending) {
            Merger.merge(hostGroupInfos, e.getHostGroups(), HostGroupInfo::new);
        }
        this.hostGroups = new HostGroups(hostGroupInfos, this.hosts);
    }

    public List<String> getLabels() {
        return labels;
    }

    public Host getLocalHost() {
        return getHost("localhost");
    }

    public Host findHost(String hostname) {
        return findHost(hostname, false);
    }

    public Host findHost(String hostname, boolean retired) {
        Host h = hosts.get(hostname);
        if (h != null && h.isRetired() != retired) {
            h = null;
        }
        return h;
    }

    public Host getHost(String hostname) {
        return getHost(hostname, false);
    }

    public Host getHost(String hostname, boolean retired) {
        Host host = findHost(hostname, retired);
        if (host == null) {
            throw new IllegalArgumentException("could not find host " + hostname);
        }
        return host;
    }

    public HostGroup findHostGroup(String hostGroupName) {
        return hostGroups.get(hostGroupName);
    }

    public HostGroup getHostGroup(String hostGroupName) {
        HostGroup hg = findHostGroup(hostGroupName);
        if (hg == null) {
            throw new IllegalArgumentException("could not find host group " + hostGroupName);
        }
        return hg;
    }

    public List<Host> queryHosts(String query) {
        return Lambda.findAll(queryAllHosts(query), h -> !h.isRetired());
    }

    public List<Host> queryAllHosts(String query) {

        QueryExpression expr = QueryExpression.parse(query);
        List<Host> list = new ArrayList<>();

        for (HostGroup hg : hostGroups) {
            if (expr.match(hg.getName())) {
                list.addAll(hg.getHosts());
            }
        }

        for (Host h : hosts) {
            if (expr.match(h.getName())) {
                list.add(h);
            }
        }

        return Lambda.unique(list);
    }

    public List<Host> filterHostsByLabels(Map<String, Object> labelsToMatch) {
        return hosts.filterByLabels(labelsToMatch);
    }

    public List<Host> filterHostsByLabels(List<String> labelsBeIncluded) {
        return hosts.filterByLabels(labelsBeIncluded);
    }

    public List<String> getGroupServers(String groupName) {
        List<Host> hs = getHostGroup(groupName).getHosts();
        List<String> servers = new ArrayList<>(hs.size());
        for (Host h : hs) {
            if (!h.isRetired()) {
                servers.add(h.getName());
            }
        }
        return servers;
    }

    public Variables getVars() {
        return vars;
    }

    public Hosts getHosts() {
        return hosts;
    }

    public HostGroups getHostGroups() {
        return hostGroups;
    }

    public String getName() {
        return name;
    }

    public boolean isLocalEnv() {
        return labels.contains(LOCAL_ENV_LABEL);
    }

    public boolean isProdEnv() {
        return labels.contains(PROD_ENV_LABEL);
    }

    public boolean isTestEnv() {
        return labels.contains(TEST_ENV_LABEL);
    }

    public String getEnvtype() {
        if (this.isProdEnv()) {
            return "prod";
        }
        if (this.isTestEnv()) {
            return "testenv";
        }
        return "local";
    }

    public String getDefaultDeployUser() {
        return defaultDeployUser;
    }

    public boolean isIncludedIn(@Nullable List<String> including, @Nullable List<String> excluding) {
        return isIncludedIn(including) && (excluding == null || !isIncludedIn(excluding));
    }

    public boolean isIncludedIn(@Nullable List<String> keys) {
        if (keys == null) {
            return true;
        }

        for (String key : keys) {
            if (isIncludedIn(key)) {
                return true;
            }
        }

        return false;
    }

    public boolean isIncludedIn(String query) {
        return new EnvironmentQuery(query).match(this);
    }

    public boolean isAbstracted() {
        return abstracted;
    }

    public String getDescription() {
        return description;
    }

    public List<Environment> getDescending() {
        return descending;
    }

    /**
     * check if the env is one of our parents or equals itself
     * @param env env to check
     * @return true if we are the child of the specified env
     */
    public boolean belongsTo(Environment env) {
        return env == null || belongsTo(env.name);
    }

    /**
     * check if the env is one of our parents or equals itself
     * @param env env to check
     * @return true if we are the child of the specified env
     */
    public boolean belongsTo(String env) {
        return env == null || Lambda.any(descending, e -> e.getName().equals(env));
    }

    public boolean match(String key) {
        return key == null
            || "all".equals(key)
            || name.equals(key)
            || labels.contains(key)
            || belongsTo(key);
    }
}