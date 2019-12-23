package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.PlayInfo;
import com.yit.deploy.core.info.TaskInfo;
import com.yit.deploy.core.function.ClosureWrapper;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class TaskContext extends BaseContext {
    public final TaskInfo task;

    public TaskContext(String path, @Nullable Closure closure, PlayInfo play) {
        if (Lambda.any(play.getTasks(), t -> t.getPath().equals(path))) {
            throw new IllegalConfigException("task " + path + " is already defined in play " + play.getName());
        }

        this.task = new TaskInfo(path);
        if (closure != null) {
            this.task.setClosure(new ClosureWrapper<>((Closure<?>) closure));
        }
        play.getTasks().add(this.task);
    }

    public TaskContext closure(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure closure) {
        task.setClosure(new ClosureWrapper<>((Closure<?>) closure));
        return this;
    }

    public TaskContext retries(int retries) {
        task.setRetries(retries);
        return this;
    }

    public TaskContext when(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<Boolean> closure) {
        task.getWhen().add(new ClosureWrapper<>(closure));
        return this;
    }

    /**
     * indicate that this task only be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public TaskContext includeOnlyEnv(String... env) {
        task.setIncludedOnlyInEnv(Arrays.asList(env));
        return this;
    }

    /**
     * indicate that this task only be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public TaskContext includeOnlyEnv(List<String> env) {
        task.setIncludedOnlyInEnv(env);
        return this;
    }

    /**
     * indicate that this play will not be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public TaskContext excludeEnv(String... env) {
        task.setExcludedInEnv(Arrays.asList(env));
        return this;
    }

    /**
     * indicate that this play will not be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public TaskContext excludeEnv(List<String> env) {
        task.setExcludedInEnv(env);
        return this;
    }

    public TaskContext tags(List<String> ts) {
        Lambda.uniqueAdd(task.getTags(), ts);
        return this;
    }

    public TaskContext tags(String ... ts) {
        return tags(Arrays.asList(ts));
    }

    public TaskContext includeRetiredHosts() {
        task.setIncludeRetiredHosts(true);
        return this;
    }

    public TaskContext onlyRetiredHosts() {
        task.setIncludeRetiredHosts(true);
        task.setOnlyRetiredHosts(true);
        return this;
    }

    public TaskContext requireResource(String resourceKey) {
        task.getResourcesRequired().add(resourceKey);
        return this;
    }

    public TaskContext reverse() {
        return reverse(true);
    }

    public TaskContext reverse(boolean value) {
        task.setReverse(value);
        return this;
    }
}