package com.yit.deploy.core.support;

import com.yit.deploy.core.docker.DockerImage;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.model.PipelineScript;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.model.ProcessLauncher;
import com.yit.deploy.core.exceptions.ProcessExecutionException;

public interface DockerSupport {

    JobExecutionContext getExecutionContext();

    default PipelineScript getScript() {
        return getExecutionContext().getScript();
    }

    default boolean imageExists(String imageFullName) {
        ProcessLauncher launcher = new ProcessLauncher().script(getScript());
        try {
            launcher.cmd("docker", "inspect", imageFullName).executeIgnoreOutput();
            return true;
        } catch (ProcessExecutionException ignore) {
            try {
                launcher.cmd("docker", "pull", imageFullName).timeout(180000).executeIgnoreOutput();
                return true;
            } catch (ProcessExecutionException ignore2) {
                return false;
            }
        }
    }

    default String determineImageTag(Host targetHost, String containerName) {
        String imageName = targetHost.ssh(getScript(), "docker inspect -f '{{.Config.Image}}' " + containerName);
        int index = imageName.lastIndexOf(':');
        if (index < 0) {
            throw new IllegalStateException("could not find image tag from image name " + imageName);
        }
        return imageName.substring(index + 1);
    }
}
