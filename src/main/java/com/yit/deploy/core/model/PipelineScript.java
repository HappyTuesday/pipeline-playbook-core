package com.yit.deploy.core.model;

import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.TaskExecutionException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.utils.GitUtils;
import com.yit.deploy.core.utils.Utils;
import groovy.lang.Closure;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.*;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * contains all implement details to access pipeline job data structures and methods
 */
public class PipelineScript implements Cloneable {

    private boolean root = true;
    private PipelineScriptSteps steps;
    private SCM scm;
    private String workspace;
    private String workspacetmp;
    private String projectsRoot;
    private EnvVars envvars;
    private String nodeName;
    private DeployUser currentUser;
    private Job job;
    private boolean sendFailureEmail = true;
    private FilePath globalRootPath;

    private String pwd;

    /**
     * for test only
     */
    public PipelineScript(String envName) {
        this(envName, "all");
    }

    /**
     * for test only
     */
    public PipelineScript(String envName, String projectName) {
        steps = new PipelineStepsImpl();
        envvars = new EnvVars();
        scm = new DummySCM();
        workspace = new File("workspace/ut/" + envName + "/" + projectName).getAbsolutePath();
        workspacetmp = workspace + "@tmp";
        projectsRoot = workspace + "@projects";
        nodeName = "master";
        currentUser = steps.getCurrentDeployUser();
        globalRootPath = new FilePath(new File(".").getAbsoluteFile());
    }

    /**
     * invoked from Jenkins Plugin
     */
    public PipelineScript(PipelineScriptSteps steps, EnvVars envvars) {
        this.steps = steps;
        this.scm = steps.getSCM();
        this.envvars = envvars;
        workspace = steps.pwd();
        workspacetmp = steps.pwd(true);
        projectsRoot = workspace + "@projects";
        nodeName = envvars.get("NODE_NAME");
        currentUser = steps.getCurrentDeployUser();
        globalRootPath = Jenkins.get().getRootPath();
    }

    public PipelineScript fork(Job job) {
        PipelineScript script;
        try {
            script = (PipelineScript) clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        script.root = false;
        script.workspace = projectsRoot + "/" + job.getProjectName();
        script.workspacetmp = script.workspace + "@tmp";
        script.projectsRoot = script.workspace + "@projects";
        script.job = job;
        script.scm = null;
        script.pwd = null;

        return script;
    }

    public void createWorkSpaceFolders() {
        try {
            if (!getWorkspaceFilePath().exists()) {
                getWorkspaceFilePath().mkdirs();
            }
            if (!getWorkspacetmpFilePath().exists()) {
                getWorkspacetmpFilePath().mkdirs();
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void warn(Object msg) {
        log("W", 100, msg);
    }

    public void warn(String format, Object ... args) {
        warn(String.format(format, args));
    }

    public void debug(Object msg) {
        log("D", 250, msg);
    }

    public void debug(String format, Object ... args) {
        debug(String.format(format, args));
    }

    public void info(Object msg) {
        log("I", 70, msg);
    }

    public void info(String format, Object ... args) {
        info(String.format(format, args));
    }

    public void echo(Object msg) {
        steps.echo(msg);
    }

    public void echo(String format, Object ... args) {
        echo(String.format(format, args));
    }

    public void error(Object msg) throws AbortException {
        steps.error(msg);
    }

    public void error(String format, Object ... args) throws AbortException {
        error(String.format(format, args));
    }

    public void log(String level, int color, Object msg) {
        steps.echo("\033[38;5;" + color + "m" + msg + "\033[m");
    }

    public void timestamp() {
        debug("TIME " + new SimpleDateFormat("HH:mm:ss.S").format(new Date()));
    }

    public void sleep(int seconds) {
        steps.sleep(seconds, "SECONDS");
    }

    public Computer getComputer() {
        return Jenkins.get().getComputer(nodeName);
    }

    public static FilePath getFilePath(String nodeName, String path) {
        if (nodeName == null || nodeName.isEmpty() || "master".equals(nodeName)) {
            return new FilePath(new File(path));
        } else {
            Computer computer = Jenkins.get().getComputer(nodeName);
            if (computer == null) {
                return new FilePath(new File(path));
            } else {
                return new FilePath(computer.getChannel(), path);
            }
        }
    }

    public static FilePath parseFilePathString(String path) {
        String nodeName = null;
        int i = path.indexOf(':');
        if (i >= 0) {
            nodeName = path.substring(0, i);
            path = path.substring(i + 1);
        }
        return getFilePath(nodeName, path);
    }

    public static String toFilePathString(FilePath path) {
        if (path.isRemote()) {
            Computer computer = Lambda.find(Jenkins.get().getComputers(), c ->
                Objects.equals(c.getChannel(), path.getChannel()));

            String nodeName = computer == null ? null : computer.getName();
            return nodeName + ":" + path.getRemote();
        } else {
            return path.getRemote();
        }
    }

    public FilePath getFilePath(String path) {
        if (nodeName == null) {
            return new FilePath(new File(path));
        } else {
            return getFilePath(nodeName, path);
        }
    }

    public FilePath getWorkspaceFilePath() {
        return getFilePath(workspace);
    }

    public FilePath getWorkspacetmpFilePath() {
        return getFilePath(workspacetmp);
    }

    public FilePath getProjectsRootFilePath() {
        return getFilePath(projectsRoot);
    }

    public FilePath createTempFile(String subfolder, String prefix) {
        return createTempFile(subfolder, prefix, null);
    }

    public FilePath createTempFile(String subfolder, String prefix, String suffix) {
        FilePath folder = getWorkspacetmpFilePath().child(subfolder);
        try {
            folder.mkdirs();
            return folder.createTempFile(prefix, suffix);
        } catch (InterruptedException e) {
            throw new ExitException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FilePath createTextTempFile(String content, String subfolder, String prefix) {
        return createTextTempFile(content, subfolder, prefix, null);
    }

    public FilePath createTextTempFile(String content, String subfolder, String prefix, String suffix) {
        FilePath folder = getWorkspacetmpFilePath().child(subfolder);
        try {
            folder.mkdirs();
            return folder.createTextTempFile(prefix, suffix, content);
        } catch (InterruptedException e) {
            throw new ExitException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FilePath createTempDir(String subfolder, String prefix) {
        return createTempDir(subfolder, prefix, null);
    }

    public FilePath createTempDir(String subfolder, String prefix, String suffix) {
        FilePath folder = getWorkspacetmpFilePath().child(subfolder);
        try {
            folder.mkdirs();
            return folder.createTempDir(prefix, suffix);
        } catch (InterruptedException e) {
            throw new ExitException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FilePath createWorkspaceTempFile(String prefix) {
        return createWorkspaceTempFile(prefix, null);
    }

    public FilePath createWorkspaceTempFile(String prefix, String suffix) {
        try {
            return getWorkspaceFilePath().createTempFile(prefix, suffix);
        } catch (InterruptedException e) {
            throw new ExitException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FilePath createWorkspaceTextTempFile(String content, String prefix) {
        return createWorkspaceTextTempFile(content, prefix, null);
    }

    public FilePath createWorkspaceTextTempFile(String content, String prefix, String suffix) {
        try {
            return getWorkspaceFilePath().createTextTempFile(prefix, suffix, content);
        } catch (InterruptedException e) {
            throw new ExitException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FilePath createWorkspaceTempDir(String prefix) {
        return createWorkspaceTempDir(prefix, null);
    }

    public FilePath createWorkspaceTempDir(String prefix, String suffix) {
        try {
            return getWorkspaceFilePath().createTempDir(prefix, suffix);
        } catch (InterruptedException e) {
            throw new ExitException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String checkoutSCM(String branch) {
        String originBranch = steps.getBranch(scm);
        steps.setBranch(scm, branch);
        try {
            steps.checkout(scm);
            return new ProcessLauncher("git", "rev-parse", "HEAD").script(this).executeReturnText();
        } finally {
            try {
                if ((originBranch == null || originBranch.isEmpty()) && job != null) {
                    originBranch = job.getDefaultBranchName();
                }
                steps.setBranch(scm, originBranch);
            } catch (Exception e) {
                warn("set default origin branch failed with error: " + e.getMessage());
            }
        }
    }

    public String checkout(String gitRepositoryUrl, String branch, FilePath path) {
        return GitUtils.checkout(gitRepositoryUrl, branch, path);
    }

    public void scheduleBuild(String jobName, Map<String, Object> parameters) {
        scheduleBuild(jobName, parameters, false);
    }

    public void scheduleBuild(String jobName, Map<String, Object> parameters, boolean waitForCompletion) {
        scheduleBuild(jobName, parameters, waitForCompletion, false);
    }

    public void scheduleBuild(String jobName, Map<String, Object> parameters, boolean waitForCompletion, boolean propagateErrors) {
        List<ParameterValue> ps = Utils.getJenkinsJobParameters(jobName, parameters);
        steps.build(jobName, ps, waitForCompletion, propagateErrors);
    }

    public String getAbsoluteUrl() {
        try {
            return steps.getRun().getAbsoluteUrl();
        } catch (UnsupportedOperationException e) {
            return "ut";
        }
    }

    public String input(String message, List<String> choices) throws AbortException {
        return steps.input(message, choices);
    }

    public String input(String message, String ... choices) throws AbortException {
        return steps.input(message, Arrays.asList(choices));
    }

    public void userConfirm(String message) {
        try {
            steps.input(message, Arrays.asList("Confirm", "Abort"));
        } catch (AbortException e) {
            throw new ExitException(e);
        }
    }

    public void userConfirm(String format, Object ... args) {
        userConfirm(String.format(format, args));
    }

    public boolean isAutoTriggered() {
        // todo find a proper way to determine if we are currently triggered by timer
        return currentUser.getEmailAddress() == null;
    }

    public DeployUser findDeployUser(String userId) {
        return steps.getDeployUser(userId);
    }

    /**
     * since we have many pipeline script instance, this field marks the instance as the first instance, and all
     * other instance are forked from this.
     */
    public boolean isRoot() {
        return root;
    }

    public PipelineScriptSteps getSteps() {
        return steps;
    }

    public SCM getScm() {
        return scm;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getWorkspacetmp() {
        return workspacetmp;
    }

    public String getProjectsRoot() {
        return projectsRoot;
    }

    public EnvVars getEnvvars() {
        return envvars;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public DeployUser getCurrentUser() {
        return currentUser;
    }

    public Job getJob() {
        return job;
    }

    public Project getProject() {
        return job == null ? null : job.getProject();
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public boolean isSendFailureEmail() {
        return sendFailureEmail;
    }

    public void setSendFailureEmail(boolean sendFailureEmail) {
        this.sendFailureEmail = sendFailureEmail;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public FilePath getGlobalRootPath() {
        return globalRootPath;
    }

    private class PipelineStepsImpl implements PipelineScriptSteps {
        @Override
        public String pwd(boolean tmp) {
            return (pwd == null ? workspace : pwd) + (tmp ? "@tmp" : "");
        }

        public <T> T stage(String name, Closure<T> closure) {
            echo("executeOnMaster stage: " + name);
            return closure.call();
        }

        @Override
        public void parallel(Map<String, Runnable> map) {
            List<Thread> threads = Lambda.map(map, (key, value) -> new Thread(() -> {
                echo("parallel thread: " + key);
                value.run();
            }, key));

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new ExitException(e);
                }
            }
        }

        @Override
        public void echo(Object obj) {
            System.out.println(obj);
        }

        @Override
        public void error(@Nonnull Object obj) throws AbortException {
            echo(obj);
            throw new AbortException(obj.toString());
        }

        @Override
        public void checkout(SCM scm) {
            DummySCM dummySCM = (DummySCM) scm;
            PipelineScript.this.checkout(job.getGitRepositoryUrl(), dummySCM.branch, getWorkspaceFilePath());
        }

        @Override
        public void dir(String path, Runnable runnable) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            String previousPwd = pwd;
            pwd = path;
            try {
                runnable.run();
            } finally {
                pwd = previousPwd;
            }
        }

        @Override
        public String getBuildCommitHash(hudson.model.Job job, Run run, String gitRepositoryUrl) {
            return null;
        }

        @Override
        public String getBranch(SCM scm) {
            return ((DummySCM) scm).branch;
        }

        @Override
        public void setBranch(SCM scm, String branch) {
            ((DummySCM) scm).branch = branch;
        }

        @Override
        public DeployUser getCurrentDeployUser() {
            return getDeployUser("ut");
        }

        @Override
        public DeployUser getDeployUser(String userId) {
            DeployUser user = new DeployUser();
            user.setId("ut");
            user.setFullName("ut");
            user.setDisplayName("ut");
            user.setEmailAddress("ut@yit.com");
            return user;
        }

        @Override
        @Nonnull
        public String input(@Nonnull String message, @Nonnull List<String> choices) {
            echo(message);
            for (String choice : choices) {
                echo(choice);
            }
            while (true) {
                String line = System.console().readLine("your choice? ").trim();
                if (choices.contains(line)) return line;
            }
        }

        @Override
        public void mail(@Nonnull Map<String, String> args) {
            echo("call mail " + args);
        }

        @Override
        public void sleepInMilliseconds(long milliseconds) throws TaskExecutionException {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                throw new ExitException(e);
            }
        }

        @Override
        @Nonnull
        public TaskListener getTaskListener() {
            return TaskListener.NULL;
        }

        @Override
        public void build(String job, List<ParameterValue> parameters, boolean wait, boolean propagate) {
            echo("build jenkins job " + job + " with " + parameters);
        }

        @Override
        @Nonnull
        public Run getRun() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SCM getSCM() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String makeLink(String url, String text) {
            return "<a href='" + url + "'>" + text + "</a>";
        }
    }

    private static class DummySCM extends SCM {
        private String repository;
        private String branch;
        private String workspace;

        /**
         * The returned object will be used to parse <tt>changelog.xml</tt>.
         */
        @Override
        public ChangeLogParser createChangeLogParser() {
            throw new UnsupportedOperationException();
        }
    }
}
