package com.yit.deploy.core.docker;

import com.yit.deploy.core.docker.containers.DockerContainer;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.exceptions.ProcessExecutionException;
import com.yit.deploy.core.global.resource.Resources;
import com.yit.deploy.core.global.task.DeferredTasks;
import com.yit.deploy.core.model.PipelineScript;
import com.yit.deploy.core.model.ProcessLauncher;
import groovy.lang.Closure;
import hudson.FilePath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nick on 24/10/2017.
 */

public class DockerImage implements Serializable {
    private static final String DOCKER_SOCK_FILE = "/var/run/docker.sock";

    private String _whoAmI;

    /**
     * while executing the container, we may need some variables. so this variable context does for us.
     */
    private final JobExecutionContext executionContext;

    /**
     * image full name, including docker registry url, folder, image name and tag
     */
    public final String imageFullName;

    /**
     * options to run docker image
     */
    private final List<String> runOptions;

    private volatile String _dockerRunFullOptions;

    private volatile String _dockerRunCommand;

    private volatile String _userHome;

    private volatile String _imageId;

    /**
     * create a docker image
     * @param executionContext variable context
     */
    public DockerImage(JobExecutionContext executionContext, String imageFullName, List<String> runOptions) {
        this.executionContext = executionContext;
        this.imageFullName = imageFullName;
        this.runOptions = runOptions;
    }

    /**
     * create a docker image
     * @param executionContext variable context
     */
    public DockerImage(JobExecutionContext executionContext, String imageFullName, String ... runOptions) {
        this(executionContext, imageFullName, Arrays.asList(runOptions));
    }

    public PipelineScript getScript() {
        return executionContext.getScript();
    }

    public Environment getEnv() {
        return executionContext.getEnv();
    }

    /**
     * create a docker container from this image.
     * NOTE: the created container does not start when this method returns, it will start as needed, in fact.
     * @return
     */
    public DockerContainer run() {
        return new DockerContainer(this);
    }

    public <T> T withRun(Closure<T> closure) {
        DockerContainer container = run();
        try {
            return closure.call(container);
        } finally {
            container.stop();
        }
    }

    public String getDockerRunFullOptions() {
        if (_dockerRunFullOptions == null) {
            List<String> options = new ArrayList<>(runOptions);
            if (getEnv().isLocalEnv()) {
                if (executionContext.getVariableOrDefault("IS_VM_JENKINS") == null) {

                    options.add("--network=" + executionContext.getVariable("DOCKER_HOST_NETWORK_NAME"));
                    options.add("--add-host=" + executionContext.getVariable("DOCKER_HOST_SERVER_DOMAIN")
                        + ":" + executionContext.getVariable("DOCKER_HOST_NETWORK_HOST_IP"));

                } else {
                    options.add("--add-host=" + executionContext.getVariable("DOCKER_HOST_SERVER_DOMAIN")
                        + ":" + executionContext.getVariable("VM_HOST_GATEWAY_IP"));
                }
            }
            options.add("-e LANG=C.UTF-8");
            _dockerRunFullOptions = String.join(" ", options);
        }
        return _dockerRunFullOptions;
    }

    private String getWhoAmI() {
        if (_whoAmI == null) {
            String uid = new ProcessLauncher("id", "-u").script(getScript()).executeReturnText();
            String gid;
            if (getEnv().getLocalHost().isDarwin()) {
                gid = new ProcessLauncher("id", "-g").script(getScript()).executeReturnText();
            } else { // currently, we use the gid of group docker to launch a container so that we can visit the /var/run/docker.sock file
                gid = new ProcessLauncher("stat", "--printf", "%g", DOCKER_SOCK_FILE).script(getScript()).executeReturnText();
            }
            _whoAmI = uid + ":" + gid;
        }
        return _whoAmI;
    }

    public String getDockerRunCommand() {
        if (_dockerRunCommand == null) {
            List<String> args = new ArrayList<>();
            args.add("docker");
            args.add("run");
            args.add("-d");
            args.add("-t");
            args.add("-u " + getWhoAmI());
            args.add("-w " + getScript().getWorkspace());
            args.add(String.format("-v %s:%s:rw,z", getScript().getWorkspace(), getScript().getWorkspace()));
            args.add(String.format("-v %s:%s:rw,z", getScript().getWorkspacetmp(), getScript().getWorkspacetmp()));
            args.add("-v /etc/passwd:/etc/passwd");
            args.add("-v /etc/group:/etc/group");
            args.add("-v " + getUserHome() + ":" + getUserHome());
            args.add("-v " + DOCKER_SOCK_FILE + ":" + DOCKER_SOCK_FILE);
            args.add("--entrypoint cat");
            args.add(getDockerRunFullOptions());
            args.add(imageFullName);
            _dockerRunCommand = String.join(" ", args);
        }
        return _dockerRunCommand;
    }

    public String getUserHome() {
        if (_userHome == null) {
            _userHome = executionContext.getVariableOrDefault("JENKINS_USER_HOME", "/var/lib/jenkins");
        }
        return _userHome;
    }

    public boolean isSnapshot() {
        return imageFullName.endsWith("-SNAPSHOT");
    }

    public void pull() {
        new ProcessLauncher("docker", "pull", imageFullName).script(getScript()).executePrintOutput();
    }

    public void push() {
        new ProcessLauncher("docker", "push", imageFullName).script(getScript()).executePrintOutput();
    }

    public void remove() {
        String imageFullName = this.imageFullName;
        FilePath pwd = getScript().getWorkspaceFilePath();

        try {
            new ProcessLauncher("docker", "inspect", imageFullName).pwd(pwd).executeIgnoreOutput();
            new ProcessLauncher("docker", "rmi", imageFullName).pwd(pwd).executeIgnoreOutput();
        } catch (ProcessExecutionException e) {
            // ignore error
        }
    }

    public static DockerImage build(JobExecutionContext executionContext, String imageFullName) {
        return build(executionContext, imageFullName, ".", "Dockerfile");
    }

    public static DockerImage build(JobExecutionContext executionContext, String imageFullName, String folderName, String dockerfile) {
        DockerImage image = new DockerImage(executionContext, imageFullName);

        new ProcessLauncher("docker", "build", "-t", imageFullName, folderName, "-f", dockerfile)
            .script(executionContext.getScript()).executePrintOutput();

        return image;
    }
}
