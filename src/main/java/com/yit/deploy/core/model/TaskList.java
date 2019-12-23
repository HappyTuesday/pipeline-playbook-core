package com.yit.deploy.core.model;

import com.yit.deploy.core.collections.ReverseList;
import com.yit.deploy.core.dsl.execute.HostExecutionContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.TaskInfo;

import java.util.*;
import java.util.function.Function;

/**
 * present a list of tasks
 */
public class TaskList {

    private final List<Task> list;

    public TaskList() {
        this.list = new ArrayList<>();
    }

    public TaskList(Iterable<TaskInfo> infos) {
        this();
        for (TaskInfo info : infos) {
            this.insertTask(info.getPath(), info);
        }
    }

    private void insertTask(String path, TaskInfo info) {
        int i = path.indexOf(Task.PATH_SPLITTER);
        if (i >= 0) {String name = path.substring(0, i);
            Task task = Lambda.find(list, t -> name.equals(t.getName()));
            if (task == null) {
                throw new IllegalConfigException("could not find parent task " + name + " for task path " + path);
            }
            task.getChildren().insertTask(path.substring(i + 1), info);
            return;
        }

        // path is a real task name
        int j = Lambda.findIndexOf(list, t -> path.equals(t.getName()));
        Task task = new Task(path, info);
        if (j < 0) {
            list.add(task);
        } else {
            list.set(j, task);
        }
    }

    public boolean isEmpty() {
        return list.isEmpty() || Lambda.all(list, Task::isEmpty);
    }

    public Set<String> getEnabledTags(Job job) {
        Set<String> tags = new HashSet<>();
        getEnabledTags(job, tags);
        return tags;
    }

    private void getEnabledTags(Job job, Set<String> tags) {
        for (Task t : list) {
            if (!t.isEmpty() && t.isEnabledIn(job)) {
                tags.addAll(t.getTags());
                t.getChildren().getEnabledTags(job, tags);
            }
        }
    }

    public void execute(DeploySpec spec, HostExecutionContext context, boolean reverseOrder, boolean root) {
        // final tasks needed to execute
        List<Task> tasks = Lambda.findAll(list, t -> t.shouldRun(spec, context));
        if (reverseOrder) {
            tasks = new ReverseList<>(tasks);
        }

        Function<Task, List<String>> requiredResourcesGetter;
        if (root) {
            requiredResourcesGetter = Task::getResourcesRequiredRecursive;
        } else {
            requiredResourcesGetter = t -> null;
        }

        Play play = context.getTargetPlay();

        ResourceOperator.using(tasks, play.getResourceOperators(), context, requiredResourcesGetter,
            task -> task.execute(spec, context));
    }

    public void getResourcesRequiredRecursive(List<String> list) {
        for (Task t : this.list) {
            t.getResourcesRequiredRecursive(list);
        }
    }

    public boolean isIncludeRetiredHosts() {
        return Lambda.any(list, Task::isIncludeRetiredHosts);
    }
}
