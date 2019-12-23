package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.dsl.execute.PlayExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.*;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.model.Play;
import com.yit.deploy.core.support.UserParameterValueTypesSupport;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.Variables;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.*;

public class PlayContext extends BaseContext implements UserParameterValueTypesSupport {
    private final List<PlaybookInfo> playbookAscending;
    private final PlayInfo play;
    private final LayeredVariables playVars;

    private List<String> currentTagsToAssign = Collections.emptyList();
    private List<Closure<Boolean>> currentWhenClosuresToAssign = Collections.emptyList();
    private List<String> currentResourcesRequiredToAssign = Collections.emptyList();
    private boolean currentIncludeRetiredHostsValue = false;

    public PlayContext(Variables contextVars, List<PlaybookInfo> playbookAscending, PlaybookInfo playbook, String playName) {
        if (playbook.getPlays().containsKey(playName)) {
            throw new IllegalConfigException("play " + playName + " is already defined in playbook " + playbook.getName());
        }

        this.playbookAscending = playbookAscending;
        this.play = new PlayInfo(playName);

        playbook.getPlays().put(playName, this.play);

        this.playVars = new LayeredVariables();
        resolveVars(contextVars, playVars);
        setWritableVars(this.play.getVars());

        updatePlayVars();
    }

    private void updatePlayVars() {
        playVars.clearLayers();
        for (PlayInfo p : this.play.descending(playbookAscending)) {
            playVars.layer(p.getVars());
        }
    }

    public void desc(String description) {
        play.setDescription(description);
    }

    public PlayContext localhost() {
        return search("localhost");
    }

    public PlayContext search(Object value) {
        setVariable(Play.SEARCH_VARIABLE, value);
        return this;
    }

    public PlayContext serial(double serial) {
        play.setSerial(serial);
        return this;
    }

    public PlayContext inheritsSuper() {
        return inherits(Play.INHERITS_SUPER);
    }

    public PlayContext inherits(String playName) {
        if (play.getName().equals(playName)) {
            throw new IllegalConfigException("could not inherits itself");
        }
        if (Play.INHERITS_SUPER.equals(playName)) {
            PlayInfo superPlay = PlaybookInfo.findSuperPlay(play, playbookAscending);
            if (superPlay == null || superPlay == play) {
                throw new IllegalConfigException("could not find play " + play.getName() + " in previous playbook");
            }
        } else {
            if (PlaybookInfo.findPlay(playName, playbookAscending) == null) {
                throw new IllegalConfigException("could not find play " + playName + " in playbook");
            }
        }

        if (this.play.getParents().contains(playName)) {
            throw new IllegalConfigException("play " + playName + " is already inherited by " + play.getName());
        }

        this.play.getParents().add(playName);
        updatePlayVars();

        return this;
    }

    public TaskContext task(String path) {
        return task(path, null);
    }

    /**
     * define a task with an optional closure at a specified path.
     * since we organize tasks of a play in a tree like way, each task associates
     * with a path consist of a set of names of the tasks from the play to it joined by '/'.
     * for instance:
     * if the path is 't1/t2/t3', it means we define a task with name 't3'
     * as a child of task 't2' which is a child of task 't1' which is a direct task of current play.
     * @param path the path of the task you want to define
     * @param closure
     * @return
     */
    public TaskContext task(String path, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure closure) {
        TaskContext context = new TaskContext(path, closure, play);

        if (!currentTagsToAssign.isEmpty()) {
            context.tags(currentTagsToAssign);
        }
        for (Closure<Boolean> c : currentWhenClosuresToAssign) {
            context.when(c);
        }
        for (String resourceKey : currentResourcesRequiredToAssign) {
            context.requireResource(resourceKey);
        }
        if (currentIncludeRetiredHostsValue) {
            context.includeRetiredHosts();
        }
        return context;
    }

    public PlayContext enablePlayWhen(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlayExecutionContext.class) Closure<Boolean> closure) {
        play.setWhen(new ClosureWrapper<>(closure));
        return this;
    }

    /**
     * indicate that this play only be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public PlayContext includeOnlyEnv(List<String> env) {
        play.setIncludedOnlyInEnv(env);
        return this;
    }

    /**
     * indicate that this play only be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public PlayContext includeOnlyEnv(String... env) {
        play.setIncludedOnlyInEnv(Arrays.asList(env));
        return this;
    }

    /**
     * indicate that this play will not be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public PlayContext excludeEnv(String... env) {
        play.setExcludedInEnv(Arrays.asList(env));
        return this;
    }

    /**
     * indicate that this play will not be included in the environments which have name/label/envtype in the env parameter
     * @param env env name/label/type list
     * @return this
     */
    public PlayContext excludeEnv(List<String> env) {
        play.setExcludedInEnv(env);
        return this;
    }

    public PlayContext alwaysRun() {
        return alwaysRun(true);
    }

    public PlayContext alwaysRun(boolean value) {
        play.setAlwaysRun(value);
        return this;
    }

    public PlayContext retries(int retries) {
        play.setRetries(retries);
        return this;
    }

    public PlayContext requirePlayResource(List<String> resourceKeys) {
        Lambda.uniqueAdd(play.getResourcesRequired(), resourceKeys);
        return this;
    }

    public PlayContext requirePlayResource(String resourceKey) {
        return requirePlayResource(Collections.singletonList(resourceKey));
    }

    /**
     * define an acquire resource method
     * @param resourceKey
     */
    public void acquireResource(String resourceKey, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure closure) {
        ResourceOperatorInfo op = play.getResourceOperators().computeIfAbsent(resourceKey, x -> new ResourceOperatorInfo());
        if (op.getAcquire() != null) {
            throw new IllegalStateException("acquire method for resource " + resourceKey + " is already defined");
        }
        op.setAcquire(new ClosureWrapper<>(closure));
    }

    /**
     * define a release resource method
     * @param resourceKey
     */
    public void releaseResource(String resourceKey, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure closure) {
        ResourceOperatorInfo op = play.getResourceOperators().computeIfAbsent(resourceKey, x -> new ResourceOperatorInfo());
        if (op.getRelease() != null) {
            throw new IllegalStateException("release method for resource " + resourceKey + " is already defined");
        }
        op.setRelease(new ClosureWrapper<>((Closure<?>) closure));
    }

    public void tag(String tag, Closure closure) {
        tags(Collections.singletonList(tag), closure);
    }

    public void tag(String tag1, String tag2, Closure closure) {
        tags(Arrays.asList(tag1, tag2), closure);
    }

    public void tag(String tag1, String tag2, String tag3, Closure closure) {
        tags(Arrays.asList(tag1, tag2, tag3), closure);
    }

    public void tags(List<String> ts, Closure closure) {
        List<String> origin = currentTagsToAssign;
        currentTagsToAssign = Lambda.concat(currentTagsToAssign, ts);
        closure.call();
        currentTagsToAssign = origin;
    }

    public void when(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<Boolean> predicate, Closure closure) {
        List<Closure<Boolean>> origin = currentWhenClosuresToAssign;
        currentWhenClosuresToAssign = Lambda.concatOne(currentWhenClosuresToAssign, predicate);
        closure.call();
        currentWhenClosuresToAssign = origin;
    }

    public void includeRetiredHosts(Closure closure) {
        boolean origin = currentIncludeRetiredHostsValue;
        currentIncludeRetiredHostsValue = true;
        closure.call();
        currentIncludeRetiredHostsValue = origin;
    }

    public void requireResource(String resourceKey, Closure closure) {
        requireResource(Collections.singletonList(resourceKey), closure);
    }

    public void requireResource(List<String> resourceKeys, Closure closure) {
        List<String> origin = currentResourcesRequiredToAssign;
        currentResourcesRequiredToAssign = Lambda.concat(currentResourcesRequiredToAssign, resourceKeys);
        closure.call();
        currentResourcesRequiredToAssign = origin;
    }
}
