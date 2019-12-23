package com.yit.deploy.core.model;

import com.yit.deploy.core.dsl.execute.HostExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.ExitPlayException;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.info.TaskInfo;

import java.util.ArrayList;
import java.util.List;

public class Task {

    public static final char PATH_SPLITTER = '/';

    private final String name;
    private final ClosureWrapper closure;
    private final TaskList children;
    private final List<ClosureWrapper<Boolean>> when;
    private final List<String> includedOnlyInEnv;
    private final List<String> excludedInEnv;
    private final int retries;
    private final List<String> tags;
    private final List<String> resourcesRequired;
    private final boolean includeRetiredHosts;
    private final boolean onlyRetiredHosts; // only used if includeRetiredHosts is true
    private final boolean reverse;

    public Task(String name, TaskInfo info) {
        this.name = name;
        this.closure = info.getClosure();
        this.children = new TaskList();
        this.when = info.getWhen();
        this.includedOnlyInEnv = info.getIncludedOnlyInEnv();
        this.excludedInEnv = info.getExcludedInEnv();
        this.retries = info.getRetries();
        this.tags = info.getTags();
        this.resourcesRequired = info.getResourcesRequired();
        this.includeRetiredHosts = info.isIncludeRetiredHosts();
        this.onlyRetiredHosts = info.isOnlyRetiredHosts();
        this.reverse = info.isReverse();
    }

    public boolean isEmpty() {
        return closure == null && children.isEmpty();
    }

    public boolean isEnabledIn(Job job) {
        return job.getEnv().isIncludedIn(includedOnlyInEnv, excludedInEnv);
    }

    public boolean shouldRun(DeploySpec spec, HostExecutionContext context) {
        if (!isEnabledIn(context.getJob())) {
            return false;
        }

        Host host = context.getTargetHost();
        for (String t : spec.tasksToSkip) {
            if (tags.contains(t)) return false;
        }
        if (host.isRetired() && !includeRetiredHosts) {
            return false;
        }
        if (includeRetiredHosts && onlyRetiredHosts && !host.isRetired()) {
            return false;
        }

        TaskExecutionContext tcx = context.toTask(this);
        for (ClosureWrapper<Boolean> c : when) {
            if (!c.withDelegateOnly(tcx)) {
                return false;
            }
        }
        // only executed if it is not empty (have a non-null closure or have some children)
        return !isEmpty();
    }

    public void execute(DeploySpec spec, HostExecutionContext context) {
        if (!isEnabledIn(context.getJob())) {
            throw new IllegalStateException("task " + name + " could not be allowed to run");
        }

        Host host = context.getTargetHost();
        PipelineScript script = context.getScript();

        script.echo("\033[1;38;5;34m" + "TASK [%s]" + "\033[m", name);
        if (context.isSingleTaskMode()) {
            script.userConfirm("confirm to execute task [%s] on host [%s (%s)]", name, host.getHostname(), host.getName());
        }

        script.timestamp();
        if (closure != null) {
            TaskExecutionContext tcx = context.toTask(this);

            for (int i = retries; i >= 0; i--) {
                try {
                    closure.delegateOnly(tcx);
                    break;
                } catch (ExitPlayException e) {
                    throw e;
                } catch (Exception e) {
                    if (ExitException.belongsTo(e)) {
                        throw ExitException.wrap(e);
                    } else if (i == 0) {
                        throw e;
                    } else {
                        script.warn("TASK [%s] failure: %s\nRETRYING ... %s times left", name, e, i);
                    }
                }
            }
        }

        if (!children.isEmpty()) {
            children.execute(spec, context, reverse, false);
        }
    }

    public List<String> getResourcesRequiredRecursive() {
        List<String> list = new ArrayList<>(resourcesRequired.size());
        getResourcesRequiredRecursive(list);
        return list;
    }

    public void getResourcesRequiredRecursive(List<String> list) {
        for (String r : resourcesRequired) {
            if (!list.contains(r)) {
                list.add(r);
            }
        }
        children.getResourcesRequiredRecursive(list);
    }

    public List<String> getTags() {
        return tags;
    }

    public TaskList getChildren() {
        return children;
    }

    public boolean isIncludeRetiredHosts() {
        return includeRetiredHosts;
    }

    public String getName() {
        return name;
    }
}
