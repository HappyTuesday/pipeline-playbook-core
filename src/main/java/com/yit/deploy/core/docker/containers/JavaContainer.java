package com.yit.deploy.core.docker.containers;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.function.Lambda;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by nick on 24/10/2017.
 */
public class JavaContainer extends DockerContainer {

    public JavaContainer(JobExecutionContext executionContext, String imageFullName) {
        this(executionContext, imageFullName, Collections.emptyList());
    }

    public JavaContainer(JobExecutionContext executionContext) {
        this(executionContext, Collections.emptyList());
    }

    public JavaContainer(JobExecutionContext executionContext, List<String> runOptions) {
        this(executionContext, executionContext.getVariable("DOCKER_REGISTRY", String.class) + "/base/openjdk:8u111-jre", runOptions);
    }

    public JavaContainer(JobExecutionContext executionContext, String imageFullName, List<String> runOptions) {
        super(executionContext, imageFullName, runOptions);
    }

    public void java(List<String> args) {
        executePrintOutput(Lambda.concat("java", args));
    }

    public void java(String ... args) {
        executePrintOutput("java", args);
    }
}
