package com.yit.deploy.core.docker.containers;

import com.yit.deploy.core.docker.DockerImage;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.support.FilePathSupport;
import com.yit.deploy.core.exceptions.ProcessExecutionException;
import com.yit.deploy.core.global.resource.Resources;
import com.yit.deploy.core.model.ContainerProcessLauncher;
import com.yit.deploy.core.model.PipelineScript;
import com.yit.deploy.core.model.ProcessExecutionStatus;
import com.yit.deploy.core.model.ProcessLauncher;
import com.yit.deploy.core.function.Lambda;
import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nick on 23/10/2017.
 */
public class DockerContainer implements Cloneable, FilePathSupport {
    protected final DockerImage image;
    private final ContainerInstance containerInstance;

    public DockerContainer(DockerImage image, ContainerInstance instance) {
        this.image = image;
        this.containerInstance = instance;
    }

    public DockerContainer(DockerImage image) {
        this(image, new ContainerInstance());
    }

    public DockerContainer(JobExecutionContext executionContext, String imageFullName, List<String> runOptions) {
        this(new DockerImage(executionContext, imageFullName, runOptions));
    }

    public DockerContainer(JobExecutionContext executionContext, String imageFullName, String ... runOptions) {
        this(executionContext, imageFullName, Arrays.asList(runOptions));
    }

    public DockerContainer(DockerContainer another) {
        this(another.image, another.containerInstance);
    }

    public PipelineScript getScript() {
        return image.getScript();
    }

    public void start() {
        synchronized (containerInstance) {
            if (containerInstance.id == null) {
                if (image.isSnapshot()) {
                    image.pull();
                }

                containerInstance.id = new ProcessLauncher()
                    .script(getScript())
                    .bash(image.getDockerRunCommand())
                    .executeReturnText()
                    .substring(0, 12);
            }
        }
    }

    public void stop() {
        synchronized (containerInstance) {
            if (containerInstance.id != null) {

                String id = containerInstance.id;
                containerInstance.id = null;

                ProcessLauncher p = new ProcessLauncher("docker", "rm", "-fv", id);
                p.pwd(getScript().getWorkspaceFilePath());

                try {
                    p.executeIgnoreOutput();
                } catch (ProcessExecutionException e) {
                    try {
                        p.executeIgnoreOutput();
                    } catch (ProcessExecutionException e2) {
                        getScript().warn("failed to stop docker container " + id, e2);
                    }
                }
            }
        }
    }

    public boolean isStarted() {
        return containerInstance.id != null;
    }

    public ProcessLauncher createProcessLauncher(String ... cmd) {
        return createProcessLauncher(Arrays.asList(cmd));
    }

    public ProcessLauncher createProcessLauncher(List<String> cmd) {
        return new ContainerProcessLauncher(getId(), cmd).getProcessLauncher().script(getScript());
    }

    public ProcessExecutionStatus execute(List<String> cmd) {
        return createProcessLauncher(cmd).executeReturnStatus();
    }

    public String executeReturnText(String bin, String ... args) {
        return executeReturnText(Lambda.concat(bin, args));
    }

    public String executeReturnText(List<String> cmd) {
        return createProcessLauncher(cmd).executeReturnText();
    }

    public void executePrintOutput(String bin, String ... args) {
        executePrintOutput(Lambda.concat(bin, args));
    }

    public void executePrintOutput(List<String> cmd) {
        createProcessLauncher(cmd).executePrintOutput();
    }

    public List<String> executeReturnLines(String bin, String ... args) {
        return executeReturnLines(Lambda.concat(bin, args));
    }

    public List<String> executeReturnLines(List<String> cmd) {
        return createProcessLauncher(cmd).executeReturnLines();
    }

    public List<String> getDockerExecCommand(List<String> cmd) {
        return Lambda.concat(Arrays.asList("docker", "exec", "-i", getId()), cmd);
    }

    public void copyFrom(String source, FilePath destination) {
        if (image.getScript().getWorkspaceFilePath().getChannel() == destination.getChannel()) {
            copyFrom(source, destination.getRemote());
        } else {
            FilePath temp = image.getScript().createTempDir("docker", "copy");
            FilePath swap = temp.child(destination.getName());

            try {
                if (destination.exists()) {
                    if (destination.isDirectory()) {
                        swap.mkdirs();
                    } else {
                        swap.touch(System.currentTimeMillis());
                    }

                    copyFrom(source, swap.getRemote());
                    FilePath swap2 = temp.child(new File(source).getName());
                    if (!swap2.getName().equals(swap.getName())) {
                        swap.renameTo(swap2);
                    }
                    copyRecursiveTo(swap2, destination);
                } else {
                    copyFrom(source, swap.getRemote());
                    copyRecursiveTo(swap, destination);
                }
            } catch (InterruptedException e) {
                throw new ExitException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void copyFrom(String source, String destination) {
        copy(getId() + ":" + source, destination);
    }

    public void copyTo(String source, String destination) {
        copy(source, getId() + ":" + destination);
    }

    public void copy(String source, String destination) {
        new ProcessLauncher("docker", "cp", source, destination).script(getScript()).executePrintOutput();
    }

    private String getId() {
        start();
        return containerInstance.id;
    }

    public DockerContainer clone() {
        try {
            return (DockerContainer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class ContainerInstance implements Serializable {
        private volatile String id;
    }
}
