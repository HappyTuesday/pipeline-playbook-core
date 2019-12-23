package com.yit.deploy.core.docker.containers;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.utils.Utils;
import hudson.FilePath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by nick on 24/10/2017.
 */
public class NodeContainer extends DockerContainer {
    private static final List<String> npmFolders = Arrays.asList("~/.npm", "~/.config", "~/.local", "~/.cache", "/usr/local/share/.cache", "/usr/local/share/.config");
    private static final List<String> npmFiles = Collections.singletonList(".npmrc");
    public List<String> options = new ArrayList<>();

    public NodeContainer(JobExecutionContext executionContext, String imageFullName, List<String> runOptions) {
        super(executionContext, imageFullName, Lambda.concat(prepareMountOptions(executionContext), runOptions));
        try {
            for (String folder : npmFolders) {
                FilePath f = executionContext.getScript().getWorkspacetmpFilePath().child(folder.replaceFirst("^~?/", ""));
                if (f.exists()) {
                    if (!f.isDirectory()) {
                        f.delete();
                        f.mkdirs();
                    }
                } else {
                    f.mkdirs();
                }
            }
            for (String file : npmFiles) {
                FilePath f = executionContext.getScript().getWorkspacetmpFilePath().child(file);
                if (f.exists()) {
                    if (f.isDirectory()) {
                        f.deleteRecursive();
                        f.write("", Utils.DefaultCharset.toString());
                    }
                } else {
                    f.write("", Utils.DefaultCharset.toString());
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public NodeContainer(JobExecutionContext executionContext) {
        this(executionContext, Collections.emptyList());
    }

    public NodeContainer(JobExecutionContext executionContext, List<String> runOptions) {
        this(executionContext, executionContext.getVariable("DOCKER_REGISTRY", String.class) + "/official/node:7.10.1-alpine", runOptions);
    }

    public NodeContainer options(List<String> options) {
        NodeContainer c = (NodeContainer) clone();
        c.options = options;
        return c;
    }

    public NodeContainer options(String ... list) {
        return options(Arrays.asList(list));
    }

    private static List<String> prepareMountOptions(JobExecutionContext executionContext) {
        List<String> list = new ArrayList<>();
        String home = executionContext.getVariable("JENKINS_USER_HOME", String.class);
        for (String f : npmFolders) {
            list.add(String.format("-v %s/%s:%s", executionContext.getScript().getWorkspacetmp(), f.replaceFirst("^~?/", ""), f.replaceFirst("^~", home)));
        }
        return list;
    }
}
