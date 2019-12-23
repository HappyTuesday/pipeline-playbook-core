package com.yit.deploy.core.workflow;

import com.yit.deploy.core.algorithm.Graph;
import com.yit.deploy.core.exceptions.DependencyFailureException;
import com.yit.deploy.core.exceptions.ExitException;
import groovy.lang.Closure;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class DependencyWorkflow<T> implements Serializable {
    private final Graph<ExecutionStatus<T>, String> graph = new Graph<>();
    private final Semaphore slotSem;

    public DependencyWorkflow() {
        this(8);
    }

    public DependencyWorkflow(int slots) {
        this.slotSem = new Semaphore(slots, true);
    }

    public DependencyWorkflow task(
        String taskName,
        T data,
        Consumer<T> work) {

        ExecutionStatus<T> status = new ExecutionStatus<>(work, data);
        graph.node(taskName, status);
        return this;
    }

    public DependencyWorkflow depends(String workName, String ... dependencies) {
        if (!graph.containsNode(workName)) throw new IllegalArgumentException(workName);
        for (String d : dependencies) {
            if (graph.containsNode(d)) {
                graph.arc(d, workName);
            }
        }
        return this;
    }

    public void executeWork(String workName) {
        ExecutionStatus<T> status = graph.getAt(workName);
        if (status == null) throw new IllegalArgumentException(workName);
        for (String prevWorkName : graph.prev(workName)) {
            ExecutionStatus<T> prevWorkStatus = graph.getAt(prevWorkName);
            prevWorkStatus.waitForFinished();
            if (prevWorkStatus.failed) {
                throw new DependencyFailureException("work " + workName + " has failed deal to the failure of its dependency " + prevWorkName);
            }
        }

        try {
            slotSem.acquire();
        } catch (InterruptedException e) {
            throw new ExitException(e);
        }
        try {
            status.work.accept(status.data);
            status.succeed();
        } catch (Exception ex) {
            status.fail();
            throw ex;
        } finally {
            slotSem.release();
        }
    }

    public List<String> topology() {
        return graph.topology();
    }

    @Override
    public String toString() {
        return graph.toString();
    }

    private static class ExecutionStatus<T> implements Serializable {
        private Consumer<T> work;
        private T data;
        private boolean success;
        private boolean failed;

        ExecutionStatus(Consumer<T> work, T data) {
            this.work = work;
            this.data = data;
        }

        synchronized void succeed() {
            success = true;
            notifyAll();
        }

        synchronized void fail() {
            failed = true;
            notifyAll();
        }

        boolean isFinished() {
            return success || failed;
        }

        synchronized void waitForFinished() {
            while (!isFinished()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new ExitException(e);
                }
            }
        }
    }
}
