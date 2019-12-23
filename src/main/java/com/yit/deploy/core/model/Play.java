package com.yit.deploy.core.model;

import com.yit.deploy.core.collections.ReverseList;
import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.dsl.evaluate.PlayEvaluationContext;
import com.yit.deploy.core.dsl.execute.HostExecutionContext;
import com.yit.deploy.core.dsl.execute.PlayExecutionContext;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.info.PlayInfo;
import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.ExitPlayException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.PlaybookInfo;
import com.yit.deploy.core.info.ResourceOperatorInfo;
import com.yit.deploy.core.inherits.Inherits;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.Variables;

import java.util.*;
import java.util.function.Function;

public class Play {
    /**
     * a host search query on whose result to execute this play
     */
    public static final String SEARCH_VARIABLE = "$PLAY_SEARCH";

    public static final String INHERITS_SUPER = "@super";

    private final String name;
    private final String description;
    private final TaskList tasks;
    private final transient LayeredVariables vars;
    private final double serial;
    private final ClosureWrapper<Boolean> when;
    private final List<String> includedOnlyInEnv;
    private final List<String> excludedInEnv;
    private final int retries;

    /**
     * just like a finally block in the try ... finally structure
     */
    private final boolean alwaysRun;
    /**
     * resource operators defined for tasks
     */
    private final Map<String, ResourceOperator> resourceOperators;

    /**
     * resources that will be acquired before play execution
     */
    private final List<String> resourcesRequired;

    public Play(PlayInfo info, List<PlaybookInfo> playbookAscending) {
        this.name = info.getName();
        this.description = info.getDescription();

        List<PlayInfo> infoDescending = info.descending(playbookAscending);
        List<PlayInfo> infoAscending = new ReverseList<>(infoDescending);

        this.tasks = new TaskList(Lambda.collectList(infoDescending, PlayInfo::getTasks));

        this.vars = new LayeredVariables();
        for (PlayInfo p : infoDescending) {
            this.vars.layer(p.getVars());
        }

        this.serial = Lambda.cascade(Lambda.cascade(infoAscending, PlayInfo::getSerial), 0.0);
        this.when = Lambda.cascade(infoAscending, PlayInfo::getWhen);
        this.includedOnlyInEnv = Lambda.cascade(infoAscending, PlayInfo::getIncludedOnlyInEnv);
        this.excludedInEnv = Lambda.cascade(infoAscending, PlayInfo::getExcludedInEnv);
        this.retries = Lambda.cascade(Lambda.cascade(infoAscending, PlayInfo::getRetries), 0);
        this.alwaysRun = Lambda.cascade(Lambda.cascade(infoAscending, PlayInfo::isAlwaysRun), false);

        this.resourceOperators = new HashMap<>();
        for (PlayInfo p : infoDescending) {
            for (Map.Entry<String, ResourceOperatorInfo> entry : p.getResourceOperators().entrySet()) {
                this.resourceOperators.put(entry.getKey(), new ResourceOperator(entry.getValue()));
            }
        }

        this.resourcesRequired = Lambda.collectList(infoDescending, PlayInfo::getResourcesRequired);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, ResourceOperator> getResourceOperators() {
        return resourceOperators;
    }

    public boolean isEnabledIn(JobEvaluationContext context) {
        if (!context.getEnv().isIncludedIn(includedOnlyInEnv, excludedInEnv)) {
            return false;
        }

        return when == null || when.withDelegateOnly(new PlayEvaluationContext(context, this));
    }

    public List<ProjectParameter> getParameters(JobEvaluationContext context) {
        return ProjectParameter.getParameters(vars, context);
    }

    public Set<String> getEnabledTags(Job job) {
        return tasks.getEnabledTags(job);
    }

    private String getSearch(PlayEvaluationContext context) {
        return context.getVariableOrDefault(SEARCH_VARIABLE, "localhost");
    }

    public List<Host> getTargetHosts(JobEvaluationContext context) {
        return context.getEnv().queryHosts(getSearch(new PlayEvaluationContext(context, this)));
    }

    public TaskList getTasks() {
        return tasks;
    }

    public void execute(DeploySpec spec, JobExecutionContext context, Function<Host, Variables> writable) {
        PlayExecutionContext pcx = context.toPlay(this);

        if (!isEnabledIn(context)) {
            context.getScript().echo("PLAY [%d] is disabled", name);
            return;
        }

        List<Host> allTargetHosts = getTargetHosts(context);

        // deploy on normal hosts
        List<Host> hosts = Lambda.findAll(allTargetHosts, Host::isNotRetired);

        final List<Host> finalHosts = Lambda.findAll(hosts, h -> spec.servers.contains(h.getName()) || h.isLocalhost());

        if (finalHosts.isEmpty() && spec.serversToRetire.isEmpty()) {
            throw new ExitException("no servers or retired servers are selected for deploying plan " + name);
        }

        if (!finalHosts.isEmpty()) {
            context.getScript().echo("\033[1;38;5;64mPLAY [%s]\033[m", name);
            executeOnHosts(spec, pcx, finalHosts, hosts, writable);
        }

        // deploy on retired hosts
        List<Host> retiredHosts = Lambda.findAll(allTargetHosts, Host::isRetired);

        if (spec.serversToRetire.isEmpty()) {
            if (retiredHosts.isEmpty()) {
                return;
            }
            // enabled only if all hosts are deployed
            if (hosts.size() != finalHosts.size()) {
                return;
            }
        }

        // enable if there are actually existing some tasks that support retired hosts
        if (!tasks.isIncludeRetiredHosts()) {
            return;
        }

        final List<Host> finalRetiredHosts = Lambda.except(retiredHosts, finalHosts);
        for (String s : spec.serversToRetire) {
            Host h = context.getEnv().getHost(s);
            if (!finalRetiredHosts.contains(h)) {
                finalRetiredHosts.add(h.toRetired(true));
            }
        }

        if (finalRetiredHosts.isEmpty()) {
            return;
        }

        context.getScript().echo("\033[1;38;5;64mPLAY [%s]:retired-hosts\033[m", name);
        executeOnHosts(spec, pcx, finalRetiredHosts, retiredHosts, writable);
    }

    private void executeOnHosts(DeploySpec spec, PlayExecutionContext context, List<Host> hosts, List<Host> hostsInGroup, Function<Host, Variables> writable) {
        Double serialOverride = context.getSerialOverride();
        double serial = serialOverride == null ? this.serial : serialOverride;

        int stepSize = (int) Math.round(hosts.size() * serial);

        if (stepSize <= 1) {
            for (Host host : hosts) {
                if (!host.isLocalhost() && context.isSingleHostMode()) {
                    context.getScript().userConfirm(
                        "confirm to execute play [%s] on host [%s (%s)]", name, host.getHostname(), host.getName());
                }
                executeOnHost(spec, context, host, hostsInGroup, writable);
            }
        } else {
            for (int i = 0; true ; i++) {
                List<Host> currentHosts = new ArrayList<>();
                for (int j = 0; j < stepSize && j + i * stepSize < hosts.size(); j++) {
                    currentHosts.add(hosts.get(j + i * stepSize));
                }
                if (currentHosts.isEmpty()) {
                    break;
                }

                Map<String, Runnable> map = new HashMap<>();
                for (Host host : currentHosts) {
                    final Host h = host;
                    if (map.containsKey(host.getHostname())) {
                        throw new IllegalStateException(
                            "there are more than one servers using the same hostname " + host.getHostname());
                    }
                    map.put(host.getHostname(), () -> executeOnHost(spec, context, h, hostsInGroup, writable));
                }

                if (serial < 1 && context.isSingleHostMode()) {
                    context.getScript().userConfirm(
                        "confirm to execute play [%s] on hosts %s",
                        name,
                        String.join(",", Lambda.map(currentHosts, Host::getHostname)));
                }
                context.getScript().getSteps().parallel(map);
            }
        }
    }

    private void executeOnHost(DeploySpec spec, PlayExecutionContext context, Host host, List<Host> hostsInGroup, Function<Host, Variables> writable) {
        HostExecutionContext hcx = context.toHost(host, hostsInGroup, writable.apply(host));

        for (int i = retries; i >= 0; i--) {
            try {
                tasks.execute(spec, hcx, false, true);
                break;
            } catch (ExitPlayException e) {
                context.getScript().echo("PLAY [%s]@%s exit with message: %s", name, host.getHostname(), e.getMessage());
                break;
            } catch (Exception e) {
                if (i == 0 || ExitException.belongsTo(e)) {
                    throw e;
                } else {
                    context.getScript().warn("PLAY [%s]@%s failed: %s\nRETRYING ... %d times left", name, host.getHostname(), e.getMessage(), i);
                }
            }
        }
    }

    public Variables getVars() {
        return vars;
    }

    public boolean isAlwaysRun() {
        return alwaysRun;
    }

    public List<String> getResourcesRequired() {
        return resourcesRequired;
    }
}
