package com.yit.deploy.core.dsl.execute;

import com.yit.deploy.core.model.Job;
import com.yit.deploy.core.model.Task;

public class TaskExecutionContext extends HostExecutionContext {

    private final Task task;

    public TaskExecutionContext(TaskExecutionContext cx) {
        this(cx, cx.task);
    }

    public TaskExecutionContext(HostExecutionContext hcx, Task task) {
        super(hcx);
        this.task = task;
    }

    @Override
    public TaskExecutionContext withJob(Job job) {
        return new TaskExecutionContext(super.withJob(job), task);
    }

    public Task getCurrentTask() {
        return task;
    }
}
