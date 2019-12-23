package com.yit.deploy.core.model;

import com.yit.deploy.core.exceptions.TaskExecutionException;
import groovy.lang.Closure;
import hudson.AbortException;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCM;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface PipelineScriptSteps {
    String pwd(boolean tmp);

    default String pwd() {
        return pwd(false);
    }

    <T> T stage(String name, Closure<T> closure);

    void parallel(Map<String, Runnable> map);

    void echo(Object obj);

    void error(@Nonnull Object obj) throws AbortException;

    void checkout(SCM scm) throws TaskExecutionException;

    void dir(String path, Runnable runnable);

    @Nonnull
    String input(@Nonnull String message, @Nonnull List<String> choices) throws TaskExecutionException, AbortException;

    void mail(@Nonnull Map<String, String> args) throws TaskExecutionException;

    default void sleep(double time, String unit) throws TaskExecutionException {
        switch (unit) {
            case "NANOSECONDS":
                time /= 1000000;
                break;
            case "MICROSECONDS":
                time /= 1000;
                break;
            case "MILLISECONDS":
                break;
            case "SECONDS":
                time *= 1000;
                break;
            case "MINUTES":
                time *= 60 * 1000;
                break;
            case "HOURS":
                time *= 60 * 60 * 1000;
                break;
            case "DAYS":
                time *= 24 * 60 * 60 * 1000;
                break;
            default:
                assert false;
        }

        sleepInMilliseconds((long) time);
    }

    void sleepInMilliseconds(long milliseconds) throws TaskExecutionException;

    @Nonnull
    TaskListener getTaskListener();

    void build(String job, List<ParameterValue> parameters, boolean wait, boolean propagate) throws TaskExecutionException;

    @Nonnull
    Run getRun();

    @Nullable
    SCM getSCM();

    String makeLink(String url, String text);

    String getBranch(SCM scm);

    void setBranch(SCM scm, String branch);

    DeployUser getCurrentDeployUser();

    DeployUser getDeployUser(String userId);

    String getBuildCommitHash(Job job, Run run, String gitRepositoryUrl);
}
